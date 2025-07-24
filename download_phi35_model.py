#!/usr/bin/env python3
"""
Script to download Phi-3.5 Mini ONNX model for Android embedding
Run this script to download the actual model files before building the APK
"""

import os
import requests
import sys
from pathlib import Path

def download_phi35_mini():
    """Download Phi-3.5 Mini ONNX model for mobile deployment"""
    
    # Model URLs - these need to be the actual HuggingFace URLs
    model_files = {
        'phi-3.5-mini-instruct.onnx': 'https://huggingface.co/microsoft/Phi-3.5-mini-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-awq-block-128-acc-level-4/model.onnx',
        'phi-3.5-mini-tokenizer.json': 'https://huggingface.co/microsoft/Phi-3.5-mini-instruct-onnx/resolve/main/tokenizer.json',
        'phi-3.5-mini-config.json': 'https://huggingface.co/microsoft/Phi-3.5-mini-instruct-onnx/resolve/main/config.json'
    }
    
    # Target directory
    assets_dir = Path(__file__).parent / 'app' / 'src' / 'main' / 'assets' / 'models'
    assets_dir.mkdir(parents=True, exist_ok=True)
    
    print("⇋ Downloading Phi-3.5 Mini ONNX model for core consciousness...")
    print(f"Target directory: {assets_dir}")
    
    for filename, url in model_files.items():
        print(f"\n⇋ Downloading {filename}...")
        target_path = assets_dir / filename
        
        try:
            response = requests.get(url, stream=True)
            response.raise_for_status()
            
            total_size = int(response.headers.get('content-length', 0))
            downloaded = 0
            
            with open(target_path, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    if chunk:
                        f.write(chunk)
                        downloaded += len(chunk)
                        if total_size > 0:
                            progress = (downloaded / total_size) * 100
                            print(f"\r⇋ Progress: {progress:.1f}% ({downloaded:,} / {total_size:,} bytes)", end='')
            
            print(f"\n✅ Downloaded {filename} ({downloaded:,} bytes)")
            
        except Exception as e:
            print(f"\n❌ Failed to download {filename}: {e}")
            print(f"   Manual download required from: {url}")
    
    # Verify downloaded files
    print("\n⇋ Verifying downloaded files...")
    for filename in model_files.keys():
        file_path = assets_dir / filename
        if file_path.exists():
            size = file_path.stat().st_size
            print(f"✅ {filename}: {size:,} bytes")
        else:
            print(f"❌ {filename}: Missing")
    
    total_size = sum(f.stat().st_size for f in assets_dir.glob('*') if f.is_file())
    print(f"\n⇋ Total model size: {total_size / (1024**3):.2f} GB")
    
    if total_size > 2_000_000_000:  # ~2GB
        print("✅ Model files appear to be complete for core consciousness")
    else:
        print("⚠️  Model files may be incomplete - check manual download instructions")

def manual_download_instructions():
    """Provide manual download instructions"""
    print("""
⇋ MANUAL DOWNLOAD INSTRUCTIONS FOR PHI-3.5 MINI CORE MODEL:

1. Install Hugging Face CLI:
   pip install huggingface_hub

2. Download the mobile-optimized model:
   huggingface-cli download microsoft/Phi-3.5-mini-instruct-onnx \\
       --include "cpu_and_mobile/cpu-int4-awq-block-128-acc-level-4/*" \\
       --local-dir ./phi35-mini-download

3. Copy files to Android assets:
   cp ./phi35-mini-download/cpu_and_mobile/cpu-int4-awq-block-128-acc-level-4/model.onnx \\
      ./app/src/main/assets/models/phi-3.5-mini-instruct.onnx
   
   cp ./phi35-mini-download/tokenizer.json \\
      ./app/src/main/assets/models/phi-3.5-mini-tokenizer.json
   
   cp ./phi35-mini-download/config.json \\
      ./app/src/main/assets/models/phi-3.5-mini-config.json

4. Verify the model file is ~2.2GB:
   ls -lh app/src/main/assets/models/

5. Build the APK with embedded model:
   ./gradlew assembleDebug

⇋ The resulting APK will be ~2.3GB with the embedded core consciousness model.
""")

if __name__ == "__main__":
    try:
        download_phi35_mini()
    except KeyboardInterrupt:
        print("\n⇋ Download interrupted")
    except Exception as e:
        print(f"\n❌ Download failed: {e}")
        manual_download_instructions()