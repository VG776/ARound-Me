from PIL import Image
import os

out_path = os.path.join(os.path.dirname(__file__), 'sample.jpg')
img = Image.new('RGB', (640, 480), color=(128, 128, 128))
img.save(out_path, format='JPEG')
print('Wrote', out_path)
