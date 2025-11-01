import argparse
import os
import sys
from typing import List

import numpy as np

try:
    import tensorflow as tf
except Exception as e:
    print("ERROR: TensorFlow is required. Please install requirements.txt first.")
    raise


def pretty_shape(shape):
    try:
        return "[" + ", ".join(str(int(s)) for s in shape) + "]"
    except Exception:
        return str(shape)


def main():
    parser = argparse.ArgumentParser(description="Inspect a TFLite model (inputs/outputs and a dry-run).")
    parser.add_argument(
        "--model",
        default=os.path.join("..", "..", "app", "src", "main", "assets", "model.tflite"),
        help="Path to the .tflite model (default: app/src/main/assets/model.tflite)",
    )
    args = parser.parse_args()

    model_path = os.path.abspath(args.model)
    if not os.path.isfile(model_path):
        print(f"ERROR: Model not found at: {model_path}")
        sys.exit(1)

    print(f"Loading TFLite model: {model_path}")
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print("\n== Inputs ==")
    for i, d in enumerate(input_details):
        print(f"  #{i}: name={d['name']}, shape={pretty_shape(d['shape'])}, dtype={d['dtype']}")

    print("\n== Outputs ==")
    for i, d in enumerate(output_details):
        print(f"  #{i}: name={d['name']}, shape={pretty_shape(d['shape'])}, dtype={d['dtype']}")

    print(f"\nOutput tensor count: {len(output_details)}")
    if len(output_details) == 4:
        print("This looks like a typical SSD-style detection model with 4 outputs (boxes, classes, scores, num_detections).")
    elif len(output_details) == 8:
        print("Model has 8 outputs. This is NOT the 4-output SSD format expected by the TF Lite Task ObjectDetector.")
        print("You likely need a custom interpreter pipeline (manual pre/post-processing) OR a different model compatible with Task API.")
    else:
        print("Model has an unexpected number of outputs. Custom handling needed.")

    # Dry-run inference with zeros to validate invocation
    print("\nRunning a dry-run inference with dummy input...")
    # Use first input tensor
    in_d = input_details[0]
    in_shape = in_d["shape"].tolist()
    # Build zero input with correct dtype
    if in_d["dtype"] == np.uint8:
        dummy = np.zeros(in_shape, dtype=np.uint8)
    else:
        dummy = np.zeros(in_shape, dtype=np.float32)

    interpreter.set_tensor(in_d["index"], dummy)
    interpreter.invoke()

    print("Dry-run OK. Output shapes:")
    for i, d in enumerate(output_details):
        out = interpreter.get_tensor(d["index"])
        print(f"  #{i}: {pretty_shape(out.shape)}")

    print("\nDone.")


if __name__ == "__main__":
    main()
