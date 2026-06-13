"""
用 optimum 重新导出 + 量化，一步到位
解决 torch.onnx.export 的 shape inference 兼容问题

安装依赖：
  pip install optimum[onnxruntime] transformers

运行：
  python fix_model.py
"""

import os
import shutil
import numpy as np

MODEL_NAME  = "textattack/bert-base-uncased-CoLA"
OUTPUT_DIR  = "./output"
ONNX_DIR    = os.path.join(OUTPUT_DIR, "onnx")
QUANT_DIR   = os.path.join(OUTPUT_DIR, "quantized")
VOCAB_PATH  = os.path.join(OUTPUT_DIR, "vocab.txt")
FINAL_ONNX  = os.path.join(OUTPUT_DIR, "grammar_model_final.onnx")

os.makedirs(OUTPUT_DIR, exist_ok=True)


# ── 1. 用 optimum 导出 ONNX（替代 torch.onnx.export）──────────────
print("Step 1: Exporting with optimum (cleaner than torch.onnx.export)...")
from optimum.onnxruntime import ORTModelForSequenceClassification

model = ORTModelForSequenceClassification.from_pretrained(
    MODEL_NAME,
    export=True          # 自动导出成 ONNX
)
model.save_pretrained(ONNX_DIR)

exported_path = os.path.join(ONNX_DIR, "model.onnx")
size_mb = os.path.getsize(exported_path) / (1024 * 1024)
print(f"  Exported → {exported_path}  ({size_mb:.1f} MB)")


# ── 2. 动态量化 ────────────────────────────────────────────────────
print("Step 2: Quantizing (INT8)...")
from optimum.onnxruntime import ORTQuantizer
from optimum.onnxruntime.configuration import AutoQuantizationConfig

quantizer = ORTQuantizer.from_pretrained(ONNX_DIR)
qconfig   = AutoQuantizationConfig.arm64(is_static=False, per_channel=False)

quantizer.quantize(
    save_dir=QUANT_DIR,
    quantization_config=qconfig
)

quantized_path = os.path.join(QUANT_DIR, "model_quantized.onnx")
size_mb = os.path.getsize(quantized_path) / (1024 * 1024)
print(f"  Quantized → {quantized_path}  ({size_mb:.1f} MB)")


# ── 3. 复制到 output 根目录，重命名 ───────────────────────────────
print("Step 3: Copying final model...")
shutil.copy(quantized_path, FINAL_ONNX)
print(f"  Final model → {FINAL_ONNX}")


# ── 4. 提取 vocab.txt ──────────────────────────────────────────────
print("Step 4: Extracting vocab.txt...")
from transformers import BertTokenizer

tokenizer = BertTokenizer.from_pretrained(MODEL_NAME)
tokenizer.save_vocabulary(OUTPUT_DIR)
print(f"  vocab.txt → {VOCAB_PATH}")


# ── 5. 验证 ────────────────────────────────────────────────────────
print("\nStep 5: Verifying...")
import onnxruntime as ort

session = ort.InferenceSession(FINAL_ONNX)

def check(sentence):
    enc = tokenizer(sentence, max_length=64, padding="max_length",
                    truncation=True, return_tensors="np")
    logits = session.run(["logits"], {
        "input_ids":      enc["input_ids"].astype(np.int64),
        "attention_mask": enc["attention_mask"].astype(np.int64),
        "token_type_ids": enc["token_type_ids"].astype(np.int64),
    })[0][0]
    probs = np.exp(logits) / np.exp(logits).sum()
    return f"grammatical={probs[1]:.2%}"

print(f"  'She go to school.'   → {check('She go to school.')}   (should be low)")
print(f"  'She goes to school.' → {check('She goes to school.')} (should be high)")
print(f"  'I have went there.'  → {check('I have went there.')}  (should be low)")
print(f"  'I went there.'       → {check('I went there.')}       (should be high)")

print("\n✅ Done! Copy these two files to app/src/main/assets/:")
print(f"   {FINAL_ONNX}")
print(f"   {VOCAB_PATH}")
