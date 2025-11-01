import argparse
import os
import sys
from typing import Tuple

import numpy as np
from PIL import Image

try:
    import tensorflow as tf
except Exception:
    print("ERROR: TensorFlow is required. Please install requirements.txt first.")
    raise


# Minimal post-processing for 4-output SSD models (boxes, classes, scores, num_detections)
# Boxes are in [ymin, xmin, ymax, xmax] normalized coordinates.

def load_image_rgb(path: str, size: Tuple[int, int]) -> np.ndarray:
    im = Image.open(path).convert("RGB").resize(size, Image.BILINEAR)
    arr = np.asarray(im)
    return arr


def main():
    parser = argparse.ArgumentParser(description="Run a quick test on an image for SSD 4-output models.")
    parser.add_argument(
        "--model",
        default=os.path.join("..", "..", "app", "src", "main", "assets", "model.tflite"),
        help="Path to the .tflite model",
    )
    parser.add_argument(
        "--image",
        required=True,
        help="Path to input image",
    )
    parser.add_argument(
        "--labels",
        default=os.path.join("..", "..", "app", "src", "main", "assets", "labelmap.txt"),
        help="Path to label file (optional)",
    )
    parser.add_argument("--score", type=float, default=0.4, help="Score threshold")

    args = parser.parse_args()
    model_path = os.path.abspath(args.model)
    image_path = os.path.abspath(args.image)

    if not os.path.isfile(model_path):
        print(f"ERROR: model not found: {model_path}")
        sys.exit(1)
    if not os.path.isfile(image_path):
        print(f"ERROR: image not found: {image_path}")
        sys.exit(1)

    labels = []
    if os.path.isfile(args.labels):
        with open(args.labels, "r", encoding="utf-8") as f:
            labels = [line.strip() for line in f if line.strip()]

    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # Try to identify SSD-style 4 outputs among possibly more outputs
    # Prefer shapes: boxes [1, N, 4], classes [1, N], scores [1, N], num [1]
    out_details = output_details

    def find_by_shape(candidates, target_rank_shape_prefix):
        for d in candidates:
            shp = d["shape"]
            if len(shp) != len(target_rank_shape_prefix):
                continue
            ok = True
            for got, want in zip(shp, target_rank_shape_prefix):
                if want is None:
                    continue
                if int(got) != int(want):
                    ok = False
                    break
            if ok:
                return d
        return None

    # Boxes: [1, N, 4]
    boxes_d = None
    for d in out_details:
        shp = d["shape"]
        if len(shp) == 3 and int(shp[0]) == 1 and int(shp[-1]) == 4:
            boxes_d = d
            break

    # num: [1]
    num_d = find_by_shape(out_details, [1])

    # Two vectors [1, N] -> classes / scores
    vecs = [d for d in out_details if len(d["shape"]) == 2 and int(d["shape"][0]) == 1]
    classes_d, scores_d = None, None

    # Heuristic: read a small inference to distinguish (scores in [0,1], classes are int-like floats)
    in_d = input_details[0]
    in_shape = in_d["shape"].tolist()
    # If the input shape is weird (like [1,1,1,3]), we'll still try but results may be meaningless.

    # Prepare a dummy or resized image tensor
    if os.path.isfile(image_path):
        # If shape is [1, H, W, 3]
        if len(in_shape) == 4 and int(in_shape[-1]) == 3:
            _, in_h, in_w, _ = in_shape
            img = load_image_rgb(image_path, (int(in_w), int(in_h)))
            if in_d["dtype"] == np.uint8:
                input_tensor = img.astype(np.uint8)
            else:
                input_tensor = (img.astype(np.float32) / 255.0).astype(np.float32)
            input_tensor = np.expand_dims(input_tensor, axis=0)
        else:
            # Unusual input; feed zeros
            if in_d["dtype"] == np.uint8:
                input_tensor = np.zeros(in_shape, dtype=np.uint8)
            else:
                input_tensor = np.zeros(in_shape, dtype=np.float32)
    else:
        print(f"ERROR: image not found: {image_path}")
        sys.exit(1)

    interpreter.set_tensor(in_d["index"], input_tensor)
    interpreter.invoke()

    # If we identified boxes and two vectors, choose which vector is scores
    if boxes_d is not None and len(vecs) >= 2 and num_d is not None:
        # Read both vectors
        v0 = interpreter.get_tensor(vecs[0]["index"])  # [1, N]
        v1 = interpreter.get_tensor(vecs[1]["index"])  # [1, N]
        # Scores likely within [0,1]
        v0_range = (float(np.min(v0)), float(np.max(v0)))
        v1_range = (float(np.min(v1)), float(np.max(v1)))
        if 0.0 <= v0_range[0] <= 1.0 and 0.0 <= v0_range[1] <= 1.0:
            scores_d, classes_d = vecs[0], vecs[1]
        elif 0.0 <= v1_range[0] <= 1.0 and 0.0 <= v1_range[1] <= 1.0:
            scores_d, classes_d = vecs[1], vecs[0]
        else:
            print("Unable to determine scores/classes vectors; aborting.")
            sys.exit(2)

        boxes = interpreter.get_tensor(boxes_d["index"])  # [1, N, 4]
        classes = interpreter.get_tensor(classes_d["index"])  # [1, N]
        scores = interpreter.get_tensor(scores_d["index"])  # [1, N]
        num = interpreter.get_tensor(num_d["index"])  # [1]

        boxes = np.squeeze(boxes, axis=0)
        classes = np.squeeze(classes, axis=0).astype(int)
        scores = np.squeeze(scores, axis=0)
        n = int(np.squeeze(num)) if np.size(num) == 1 else boxes.shape[0]

        print(f"Detections (score >= {args.score}):")
        for i in range(n):
            if i >= boxes.shape[0] or i >= classes.shape[0] or i >= scores.shape[0]:
                break
            if scores[i] < args.score:
                continue
            cls_idx = classes[i]
            label = labels[cls_idx] if 0 <= cls_idx < len(labels) else str(cls_idx)
            ymin, xmin, ymax, xmax = boxes[i]
            print(f"  {i:02d}: {label:>12s}  score={scores[i]:.2f}  box=[ymin={ymin:.2f}, xmin={xmin:.2f}, ymax={ymax:.2f}, xmax={xmax:.2f}]")
    else:
        print("Could not find typical 4-output detection tensors inside this model. Unsupported layout.")
        sys.exit(3)


if __name__ == "__main__":
    main()
