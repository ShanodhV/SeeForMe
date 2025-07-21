"""
YOLO to TensorFlow Lite Conversion Script for Mobile Blind Assistant

This script converts a YOLO model to TensorFlow Lite format optimized for mobile devices.
It includes quantization for better performance on mobile hardware.

Requirements:
- ultralytics
- tensorflow
- numpy
- opencv-python

Usage:
python convert_yolo_to_tflite.py --model yolov8n.pt --output yolo_mobile_model.tflite
"""

import argparse
import tensorflow as tf
from ultralytics import YOLO
import numpy as np
import cv2
import os

def convert_yolo_to_tflite(model_path, output_path, quantize=True, input_size=320):
    """
    Convert YOLO model to TensorFlow Lite format
    
    Args:
        model_path: Path to YOLO .pt model
        output_path: Path for output .tflite model
        quantize: Whether to apply quantization (recommended for mobile)
        input_size: Input image size (320x320 is good for mobile)
    """
    
    print(f"Loading YOLO model from {model_path}...")
    model = YOLO(model_path)
    
    # Export to TensorFlow format first
    tf_model_path = model_path.replace('.pt', '_tf_model')
    print(f"Exporting to TensorFlow format...")
    model.export(format='tensorflow', imgsz=input_size)
    
    # Load the TensorFlow model
    print("Loading TensorFlow model...")
    tf_model = tf.saved_model.load(tf_model_path)
    
    # Get the concrete function
    concrete_func = tf_model.signatures['serving_default']
    
    # Create TensorFlow Lite converter
    converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_func])
    
    # Configure converter for mobile optimization
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    if quantize:
        print("Applying quantization for mobile optimization...")
        
        # Generate representative dataset for quantization
        def representative_dataset():
            for _ in range(100):
                # Generate random sample data
                data = np.random.random((1, input_size, input_size, 3)).astype(np.float32)
                yield [data]
        
        converter.representative_dataset = representative_dataset
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        converter.inference_input_type = tf.uint8
        converter.inference_output_type = tf.uint8
    
    # Convert the model
    print("Converting to TensorFlow Lite...")
    tflite_model = converter.convert()
    
    # Save the model
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"✅ Conversion complete! Model saved to {output_path}")
    
    # Get model size
    model_size_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"📱 Model size: {model_size_mb:.2f} MB")
    
    # Test the converted model
    print("🧪 Testing converted model...")
    test_tflite_model(output_path, input_size)

def test_tflite_model(model_path, input_size):
    """Test the converted TensorFlow Lite model"""
    
    # Load TFLite model and allocate tensors
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    
    # Get input and output tensors
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print(f"Input shape: {input_details[0]['shape']}")
    print(f"Output shape: {output_details[0]['shape']}")
    
    # Test with dummy data
    input_data = np.random.random((1, input_size, input_size, 3)).astype(np.float32)
    
    if input_details[0]['dtype'] == np.uint8:
        input_data = (input_data * 255).astype(np.uint8)
    
    interpreter.set_tensor(input_details[0]['index'], input_data)
    
    # Run inference
    interpreter.invoke()
    
    # Get output
    output_data = interpreter.get_tensor(output_details[0]['index'])
    print(f"✅ Model test successful! Output shape: {output_data.shape}")

def create_optimized_models():
    """Create multiple optimized versions of the model"""
    
    # Model configurations for different use cases
    configs = [
        {
            'name': 'yolo_mobile_fast.tflite',
            'input_size': 320,
            'quantize': True,
            'description': 'Fast inference, lower accuracy'
        },
        {
            'name': 'yolo_mobile_balanced.tflite', 
            'input_size': 416,
            'quantize': True,
            'description': 'Balanced speed and accuracy'
        },
        {
            'name': 'yolo_mobile_accurate.tflite',
            'input_size': 640,
            'quantize': False,
            'description': 'Higher accuracy, slower inference'
        }
    ]
    
    base_model = 'yolov8n.pt'  # Nano version for mobile
    
    if not os.path.exists(base_model):
        print("⬇️ Downloading YOLOv8 nano model...")
        model = YOLO(base_model)  # This will download if not exists
    
    for config in configs:
        print(f"\n🔄 Creating {config['name']} - {config['description']}")
        convert_yolo_to_tflite(
            base_model, 
            config['name'],
            quantize=config['quantize'],
            input_size=config['input_size']
        )

def main():
    parser = argparse.ArgumentParser(description='Convert YOLO to TensorFlow Lite for mobile')
    parser.add_argument('--model', default='yolov8n.pt', help='Path to YOLO model')
    parser.add_argument('--output', default='yolo_mobile_model.tflite', help='Output TFLite model path')
    parser.add_argument('--input-size', type=int, default=320, help='Input image size')
    parser.add_argument('--no-quantize', action='store_true', help='Disable quantization')
    parser.add_argument('--create-all', action='store_true', help='Create all optimized variants')
    
    args = parser.parse_args()
    
    if args.create_all:
        create_optimized_models()
    else:
        convert_yolo_to_tflite(
            args.model,
            args.output, 
            quantize=not args.no_quantize,
            input_size=args.input_size
        )

if __name__ == '__main__':
    main()
