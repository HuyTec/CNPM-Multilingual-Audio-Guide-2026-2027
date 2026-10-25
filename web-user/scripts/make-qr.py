"""Generate an actual, locally encoded QR; no external QR image service."""
import argparse
from pathlib import Path
import sys
from urllib.parse import urlparse

root = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(root / '.local-tools' / 'python'))
import qrcode
from qrcode.image.svg import SvgPathImage

parser = argparse.ArgumentParser()
parser.add_argument('url')
parser.add_argument('--output', default=str(root / 'docs' / 'qr' / 'web-user-demo'))
args = parser.parse_args()
url = urlparse(args.url)
if url.scheme != 'https' or not url.hostname or url.hostname in ('localhost', '127.0.0.1'):
    parser.error('Use a public HTTPS URL for phone access and GPS.')
output = Path(args.output).resolve()
output.parent.mkdir(parents=True, exist_ok=True)
qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=12, border=4)
qr.add_data(args.url)
qr.make(fit=True)
qr.make_image(fill_color='black', back_color='white').save(str(output) + '.png')
qr.make_image(image_factory=SvgPathImage).save(str(output) + '.svg')
Path(str(output) + '.txt').write_text(args.url + '\n', encoding='utf-8')
print(str(output) + '.png')
