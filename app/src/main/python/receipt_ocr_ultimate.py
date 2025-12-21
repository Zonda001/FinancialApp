"""
receipt_ocr_ultimate.py - ULTIMATE WORKING VERSION

This version:
- Uses actual OCR results (not guessing)
- Better Ukrainian text recognition
- Smarter field matching
- Works with your specific receipt format

Usage:
    python receipt_ocr_ultimate.py --image receipt.jpg
"""

import cv2
import numpy as np
from PIL import Image
import json
import argparse
from pathlib import Path
import re


class UltimateReceiptOCR:
    """Final working OCR version"""

    def __init__(self, languages=['uk', 'en', 'ru']):
        print("Loading EasyOCR...")
        try:
            import easyocr
            # Put Ukrainian FIRST for better recognition
            self.reader = easyocr.Reader(languages, gpu=True, verbose=False)
            print(f"Ready! Languages: {', '.join(languages)}")
        except ImportError:
            print("ERROR: pip install easyocr")
            exit(1)

    def process_image(self, image_path):
        """Simple processing - don't destroy the image!"""
        print(f"\nProcessing: {image_path}")

        img = cv2.imread(str(image_path))
        if img is None:
            print(f"ERROR: Cannot load {image_path}")
            return []

        img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        print(f"  Image size: {img.shape[1]}x{img.shape[0]}")

        # Minimal preprocessing - just denoise
        if len(img.shape) == 3:
            gray = cv2.cvtColor(img, cv2.COLOR_RGB2GRAY)
        else:
            gray = img

        # Very light denoise
        clean = cv2.fastNlMeansDenoising(gray, h=5)

        # Save debug
        debug_path = Path(image_path).with_suffix('.processed.jpg')
        cv2.imwrite(str(debug_path), clean)
        print(f"  Saved: {debug_path.name}")

        # Run OCR
        print("  Running OCR...")
        results = self.reader.readtext(clean)

        # Format
        ocr_data = []
        for bbox, text, conf in results:
            x1 = min(p[0] for p in bbox)
            y1 = min(p[1] for p in bbox)
            x2 = max(p[0] for p in bbox)
            y2 = max(p[1] for p in bbox)

            ocr_data.append({
                'text': text,
                'confidence': round(float(conf), 3),
                'x': int(x1),
                'y': int(y1),
                'width': int(x2 - x1),
                'height': int(y2 - y1)
            })

        # Sort by Y position (top to bottom)
        ocr_data.sort(key=lambda r: r['y'])

        print(f"  Found {len(ocr_data)} text regions")
        filtered = [r for r in ocr_data if r['confidence'] > 0.3]
        print(f"  High confidence: {len(filtered)}")

        return filtered

    def is_number(self, text):
        """Check if text is a number (price/amount)"""
        # Remove spaces, dots, commas
        clean = text.replace(' ', '').replace(',', '.').replace('Г', '').replace('Р', '').replace('Н', '')
        clean = re.sub(r'[^\d.]', '', clean)

        if not clean:
            return False

        # Must have at least 50% digits
        digit_ratio = sum(c.isdigit() for c in text) / len(text)
        return digit_ratio > 0.4

    def clean_number(self, text):
        """Extract clean number from text"""
        # Remove currency symbols and letters
        clean = re.sub(r'[^\d.,\s-]', '', text)
        clean = clean.strip().replace(' ', '')

        # Replace comma with dot if it looks like decimal separator
        if ',' in clean and clean.count(',') == 1:
            parts = clean.split(',')
            if len(parts) == 2 and len(parts[1]) == 2:
                clean = clean.replace(',', '.')

        return clean

    def find_on_same_line(self, ocr_data, y_position, tolerance=30, x_min=None):
        """Find text on same horizontal line"""
        results = []
        for item in ocr_data:
            if abs(item['y'] - y_position) < tolerance:
                if x_min is None or item['x'] > x_min:
                    results.append(item)
        return results

    def structure_receipt(self, ocr_data):
        """Smart receipt structuring for Ukrainian receipts"""

        receipt = {
            'items': [],
            'suma': None,  # Ukrainian СУМА (total)
            'pdv': None,   # Ukrainian ПДВ (VAT/tax)
            'discount': None,
            'total': None,
            'do_splaty': None,  # До сплати (to pay)
            'bezgotivkova': None,  # Безготівкова (card payment)
            'raw_text': [r['text'] for r in ocr_data]
        }

        # Process each line
        for i, item in enumerate(ocr_data):
            text = item['text']
            text_upper = text.upper()
            y = item['y']
            x = item['x']

            # Find numbers on same line
            numbers_on_line = [
                r for r in self.find_on_same_line(ocr_data, y)
                if self.is_number(r['text']) and r['x'] > x
            ]

            # Ukrainian keywords (with OCR error tolerance)
            if any(word in text_upper for word in ['СУМА', 'SUMA', 'СЧМА', 'СYMA', 'CYMА']):
                if numbers_on_line:
                    receipt['suma'] = self.clean_number(numbers_on_line[0]['text'])
                    print(f"  Found СУМА: {receipt['suma']}")

            elif 'ПДВ' in text_upper or 'PDV' in text_upper or 'VAT' in text_upper:
                if numbers_on_line:
                    receipt['pdv'] = self.clean_number(numbers_on_line[0]['text'])
                    print(f"  Found ПДВ: {receipt['pdv']}")

            elif any(word in text_upper for word in ['СПЛАТИ', 'СПЛАТ', 'СПХІАТИ', 'CNЛАТИ']):
                # "ДО СПЛАТИ" = amount to pay
                if numbers_on_line:
                    receipt['do_splaty'] = self.clean_number(numbers_on_line[0]['text'])
                    print(f"  Found ДО СПЛАТИ: {receipt['do_splaty']}")

            elif 'БЕЗГОТІВК' in text_upper or 'КАРТК' in text_upper or 'CARD' in text_upper:
                # Card payment
                if numbers_on_line:
                    receipt['bezgotivkova'] = self.clean_number(numbers_on_line[0]['text'])
                    print(f"  Found БЕЗГОТІВКОВА: {receipt['bezgotivkova']}")

            elif 'ЗНИЖК' in text_upper or 'DISCOUNT' in text_upper:
                # Discount
                if numbers_on_line:
                    receipt['discount'] = self.clean_number(numbers_on_line[0]['text'])
                    print(f"  Found ЗНИЖКА: {receipt['discount']}")

            elif 'TOTAL' in text_upper:
                if numbers_on_line:
                    receipt['total'] = self.clean_number(numbers_on_line[0]['text'])
                    print(f"  Found TOTAL: {receipt['total']}")

            # Detect items (left-aligned text with price on right)
            elif x < 600 and len(text) > 3:  # Left side of receipt
                # Must have mostly letters
                letter_ratio = sum(c.isalpha() or c in 'іїєґ' for c in text) / len(text)

                if letter_ratio > 0.5:
                    # Look for price on same line
                    price = None
                    for num_item in numbers_on_line:
                        # Price should be on right side
                        if num_item['x'] > x + 200:
                            price = self.clean_number(num_item['text'])
                            break

                    if price:
                        receipt['items'].append({
                            'name': text,
                            'price': price,
                            'confidence': item['confidence']
                        })

        return receipt

    def save_json(self, data, output_path):
        """Save to JSON"""
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print(f"\nSaved: {output_path}")


def main():
    parser = argparse.ArgumentParser(description='Ultimate Receipt OCR')
    parser.add_argument('--image', type=str, required=True)
    parser.add_argument('--output', type=str)
    parser.add_argument('--lang', nargs='+', default=['uk', 'en', 'ru'])

    args = parser.parse_args()

    # Create OCR
    ocr = UltimateReceiptOCR(languages=args.lang)

    # Process
    ocr_data = ocr.process_image(args.image)

    if not ocr_data:
        print("\nNo text detected!")
        return

    # Structure
    print("\nStructuring receipt...")
    receipt = ocr.structure_receipt(ocr_data)

    # Save
    output_data = {
        'receipt': receipt,
        'raw_ocr': ocr_data
    }

    if args.output:
        output_path = args.output
    else:
        output_path = Path(args.image).with_suffix('.result.json')

    ocr.save_json(output_data, output_path)

    # Print nice summary
    print("\n" + "="*60)
    print("ЧЕКІК")
    print("="*60)

    # Items
    if receipt['items']:
        print(f"\nТовари ({len(receipt['items'])}):")
        for i, item in enumerate(receipt['items'], 1):
            name = item['name'][:40]
            price = item.get('price', 'N/A')
            conf = item.get('confidence', 0)
            print(f"  {i}. {name:40s} {price:>12s} [{conf:.2f}]")
    else:
        print("\nТовари: не знайдено")

    # Totals
    print(f"\nПідсумки:")

    fields = [
        ('Сума', receipt['suma']),
        ('ПДВ', receipt['pdv']),
        ('Знижка', receipt['discount']),
        ('До сплати', receipt['do_splaty']),
        ('Безготівкова', receipt['bezgotivkova']),
        ('Total', receipt['total'])
    ]

    for label, value in fields:
        if value:
            print(f"  {label:15s} {value}")

    # All text found
    print(f"\nВесь текст ({len(ocr_data)}):")
    for i, item in enumerate(ocr_data[:25], 1):
        conf = item['confidence']
        text = item['text'][:50]
        print(f"  {i:2d}. [{conf:.2f}] {text}")

    if len(ocr_data) > 25:
        print(f"  ... і ще {len(ocr_data) - 25}")

    print("\n" + "="*60)
    print(f"Результати: {output_path}")
    print("="*60)


if __name__ == '__main__':
    main()