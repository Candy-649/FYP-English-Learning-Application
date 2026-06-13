"""
把 HuggingFace BERT 模型转成 ONNX 格式（不需要 TensorFlow！）
支持 Python 3.13，只需要 PyTorch

安装依赖：
  pip install torch transformers onnx optimum

运行：
  python convert_model.py

输出文件放进 app/src/main/assets/：
  grammar_model.onnx
  vocab.txt
"""

import os
import numpy as np
import torch
from transformers import BertTokenizer, BertForSequenceClassification

MODEL_NAME  = "textattack/bert-base-uncased-CoLA"
OUTPUT_DIR  = "./output"
ONNX_PATH   = os.path.join(OUTPUT_DIR, "grammar_model.onnx")
VOCAB_PATH  = os.path.join(OUTPUT_DIR, "vocab.txt")
MAX_SEQ_LEN = 64

os.makedirs(OUTPUT_DIR, exist_ok=True)


# ── 1. 下载模型 ────────────────────────────────────────────────────
print("Downloading model (PyTorch)...")
tokenizer = BertTokenizer.from_pretrained(MODEL_NAME)
model     = BertForSequenceClassification.from_pretrained(MODEL_NAME)
model.eval()

tokenizer.save_vocabulary(OUTPUT_DIR)
print(f"Vocab saved → {VOCAB_PATH}")


# ── 2. 准备 dummy 输入用于 tracing ─────────────────────────────────
dummy_input = tokenizer(
    "This is a test sentence.",
    max_length=MAX_SEQ_LEN,
    padding="max_length",
    truncation=True,
    return_tensors="pt"
)

input_ids      = dummy_input["input_ids"]
attention_mask = dummy_input["attention_mask"]
token_type_ids = dummy_input["token_type_ids"]


# ── 3. 导出 ONNX ───────────────────────────────────────────────────
print("Exporting to ONNX...")
with torch.no_grad():
    torch.onnx.export(
        model,
        args=(input_ids, attention_mask, token_type_ids),
        f=ONNX_PATH,
        input_names=["input_ids", "attention_mask", "token_type_ids"],
        output_names=["logits"],
        dynamic_axes={
            "input_ids":      {0: "batch"},
            "attention_mask": {0: "batch"},
            "token_type_ids": {0: "batch"},
            "logits":         {0: "batch"},
        },
        opset_version=14,
        do_constant_folding=True,
    )

size_mb = os.path.getsize(ONNX_PATH) / (1024 * 1024)
print(f"ONNX model saved → {ONNX_PATH}  ({size_mb:.1f} MB)")


# ── 4. 验证 ONNX 结构 ──────────────────────────────────────────────
import onnx
onnx_model = onnx.load(ONNX_PATH)
onnx.checker.check_model(onnx_model)
print("ONNX model structure: OK")


# ── 5. 用 ONNX Runtime 验证推理结果 ───────────────────────────────
print("\nVerifying with ONNX Runtime...")
try:
    import onnxruntime as ort

    session = ort.InferenceSession(ONNX_PATH)

    def quick_check(sentence: str) -> str:
        enc = tokenizer(
            sentence,
            max_length=MAX_SEQ_LEN,
            padding="max_length",
            truncation=True,
            return_tensors="np"
        )
        logits = session.run(
            ["logits"],
            {
                "input_ids":      enc["input_ids"].astype(np.int64),
                "attention_mask": enc["attention_mask"].astype(np.int64),
                "token_type_ids": enc["token_type_ids"].astype(np.int64),
            }
        )[0][0]
        probs = np.exp(logits) / np.exp(logits).sum()
        return f"grammatical={probs[1]:.2%}  ungrammatical={probs[0]:.2%}"

    print(f"  'She go to school.'          → {quick_check('She go to school.')}")
    print(f"  'She goes to school.'        → {quick_check('She goes to school.')}")
    print(f"  'I have went to the store.'  → {quick_check('I have went to the store.')}")
    print(f"  'I went to the store.'       → {quick_check('I went to the store.')}")

except ImportError:
    print("  (onnxruntime not installed, skipping runtime check)")
    print("  Install with: pip install onnxruntime")


print("\nDone! Copy these files to app/src/main/assets/:")
print(f"  {ONNX_PATH}")
print(f"  {VOCAB_PATH}")
