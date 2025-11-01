import numpy as np
import tensorflow as tf
from PIL import Image

# Path to YOLOv5 or YOLOv8 TFLite model (replace with your actual model path)
MODEL_PATH = "../../app/src/main/assets/yolov5.tflite"  # Update this if you have yolov8 or a different path

# Load TFLite model and allocate tensors
interpreter = tf.lite.Interpreter(model_path=MODEL_PATH)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Print model input/output details
print("Input details:", input_details)
print("Output details:", output_details)

# Load and preprocess a sample image (replace with your test image path)
def preprocess_image(image_path, input_shape):
    img = Image.open(image_path).convert('RGB')
    img = img.resize((input_shape[2], input_shape[1]))
    img = np.array(img, dtype=np.float32)
    img = img / 255.0  # Normalize if required by model
    img = np.expand_dims(img, axis=0)
    return img

# Example usage
if __name__ == "__main__":
    # Replace with your test image path
    image_path = "../../app/src/main/assets/test.jpg"
    input_shape = input_details[0]['shape']
    input_data = preprocess_image(image_path, input_shape)

    interpreter.set_tensor(input_details[0]['index'], input_data)
    interpreter.invoke()
    output_data = interpreter.get_tensor(output_details[0]['index'])
    print("Output data shape:", output_data.shape)
    print("Output data (first 5):", output_data.flatten()[:5])
