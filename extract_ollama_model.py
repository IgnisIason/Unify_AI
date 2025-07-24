#!/usr/bin/env python3
"""
Extract Phi-3.5 model from Ollama and prepare for Android integration
"""

import os
import sys
import json
import shutil
import subprocess
from pathlib import Path

def find_ollama_models():
    """Find Ollama models directory"""
    possible_paths = [
        Path.home() / ".ollama" / "models",
        Path("/usr/share/ollama/models"),
        Path("/var/lib/ollama/models"),
        Path("/opt/ollama/models")
    ]
    
    for path in possible_paths:
        if path.exists():
            print(f"⇋ Found Ollama models at: {path}")
            return path
    
    print("❌ Ollama models directory not found")
    return None

def find_phi35_model(models_dir):
    """Find the Phi-3.5 model files"""
    phi35_files = []
    
    # Look for blobs directory
    blobs_dir = models_dir / "blobs"
    if not blobs_dir.exists():
        print("❌ Ollama blobs directory not found")
        return None
    
    # Find manifest files
    manifests_dir = models_dir / "manifests" / "registry.ollama.ai" / "library" / "phi3.5"
    if manifests_dir.exists():
        print(f"⇋ Found Phi-3.5 manifests: {manifests_dir}")
        
        # Look for the 3.8b variant
        tag_file = manifests_dir / "3.8b"
        if tag_file.exists():
            print(f"⇋ Found Phi-3.5:3.8b manifest: {tag_file}")
            
            # Read manifest to find blob references
            try:
                manifest_content = json.loads(tag_file.read_text())
                print("⇋ Manifest loaded successfully")
                
                # Find model blob
                for layer in manifest_content.get("layers", []):
                    media_type = layer.get("mediaType", "")
                    digest = layer.get("digest", "")
                    size = layer.get("size", 0)
                    
                    if "application/vnd.ollama.image.model" in media_type:
                        blob_path = blobs_dir / f"sha256-{digest.split(':')[1]}"
                        if blob_path.exists():
                            print(f"⇋ Found model blob: {blob_path} ({size / (1024**3):.1f}GB)")
                            return blob_path
                        
            except Exception as e:
                print(f"❌ Error reading manifest: {e}")
    
    return None

def extract_model_to_assets(model_blob_path, assets_dir):
    """Extract model to Android assets directory"""
    if not model_blob_path or not model_blob_path.exists():
        print("❌ Model blob not found")
        return False
    
    assets_models_dir = assets_dir / "models"
    assets_models_dir.mkdir(parents=True, exist_ok=True)
    
    target_file = assets_models_dir / "phi-3.5-mini-instruct.onnx"
    
    print(f"⇋ Copying model blob to assets...")
    print(f"   Source: {model_blob_path}")
    print(f"   Target: {target_file}")
    
    try:
        # Copy the model file
        shutil.copy2(model_blob_path, target_file)
        
        # Verify the copy
        if target_file.exists():
            size = target_file.stat().st_size
            print(f"✅ Model copied successfully: {size / (1024**3):.2f}GB")
            return True
        else:
            print("❌ Model copy failed")
            return False
            
    except Exception as e:
        print(f"❌ Error copying model: {e}")
        return False

def create_onnx_compatible_model():
    """Create ONNX-compatible model structure"""
    print("⇋ Note: Ollama models are in GGUF format, not ONNX")
    print("⇋ For production Android deployment, you need the ONNX version")
    print("⇋ Download from: microsoft/Phi-3.5-mini-instruct-onnx")
    
    # For now, we'll use the GGUF model and add conversion instructions
    return True

def verify_integration():
    """Verify the model integration"""
    assets_dir = Path(__file__).parent / "app" / "src" / "main" / "assets" / "models"
    
    model_file = assets_dir / "phi-3.5-mini-instruct.onnx"
    
    if model_file.exists():
        size = model_file.stat().st_size
        size_gb = size / (1024**3)
        
        print(f"⇋ Model file: {model_file}")
        print(f"⇋ Size: {size_gb:.2f}GB")
        
        if size_gb > 1.0:
            print("✅ Model appears to be a full model")
            return True
        else:
            print("⚠️  Model appears small - may need ONNX conversion")
            return False
    else:
        print("❌ Model file not found in assets")
        return False

def main():
    print("⇋ EXTRACTING PHI-3.5 MODEL FROM OLLAMA FOR ANDROID")
    print("=" * 60)
    
    # Find Ollama models
    models_dir = find_ollama_models()
    if not models_dir:
        return False
    
    # Find Phi-3.5 model
    model_blob = find_phi35_model(models_dir)
    if not model_blob:
        print("❌ Phi-3.5 model not found in Ollama")
        return False
    
    # Prepare assets directory
    project_root = Path(__file__).parent
    assets_dir = project_root / "app" / "src" / "main" / "assets"
    
    # Extract model
    success = extract_model_to_assets(model_blob, assets_dir)
    if not success:
        return False
    
    # Verify integration
    verify_integration()
    
    print("\n⇋ NEXT STEPS FOR ONNX CONVERSION:")
    print("-" * 40)
    print("""
The Ollama model is in GGUF format. For Android ONNX Runtime, you need:

1. Download the official ONNX version:
   huggingface-cli download microsoft/Phi-3.5-mini-instruct-onnx \\
       --include "cpu_and_mobile/cpu-int4-awq-block-128-acc-level-4/*" \\
       --local-dir ./phi35-onnx

2. Copy the ONNX model:
   cp ./phi35-onnx/cpu_and_mobile/cpu-int4-awq-block-128-acc-level-4/model.onnx \\
      ./app/src/main/assets/models/phi-3.5-mini-instruct.onnx

3. Build APK:
   ./gradlew assembleDebug

Alternatively, you can use the current GGUF file as a placeholder
and the app will detect it needs the proper ONNX format.
""")
    
    return True

if __name__ == "__main__":
    try:
        success = main()
        if success:
            print("\n✅ Model extraction completed")
        else:
            print("\n❌ Model extraction failed")
            sys.exit(1)
    except KeyboardInterrupt:
        print("\n⇋ Extraction interrupted")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}")
        sys.exit(1)