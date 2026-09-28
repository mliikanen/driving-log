# PP-OCR model files

The seven-segment reading recognizer (`add-seven-segment-ocr`) runs these on ONNX Runtime. Sources: PaddleOCR's models in the ONNX
conversions RapidOCR publishes (release v3.9.2, `https://www.modelscope.cn/models/RapidAI/RapidOCR/resolve/v3.9.2/onnx/...`), Apache
License 2.0 (see `THIRD_PARTY_NOTICES.md`).

| File | What it is |
|---|---|
| `PP-OCRv6_det_tiny.onnx` | `onnx/PP-OCRv6/det/PP-OCRv6_det_tiny.onnx`, unmodified. |
| `en_PP-OCRv5_rec_mobile.onnx` | `onnx/PP-OCRv5/rec/en_PP-OCRv5_rec_mobile.onnx`, with its 28 `HardSwish` nodes each rewritten as `x * HardSigmoid(x, alpha=1/6, beta=0.5)` (the same function; outputs are bit-identical on ONNX Runtime 1.30 in Python). ONNX Runtime's JVM build (1.22 to 1.30, Linux x86-64) returns zeros for `HardSwish`, which made the original read nothing there. |
| `en_PP-OCRv5_rec_mobile.characters.txt` | The recognizer's character list, one per line, taken from the original model's `character` metadata. It is shipped as a file because ONNX Runtime's Java API garbles metadata characters outside the Basic Multilingual Plane (three entries collapsed, shifting every later class). |

The rewrite, in Python with the `onnx` package:

```python
import onnx
from onnx import helper
m = onnx.load("en_PP-OCRv5_rec_mobile.onnx")
nodes = []
for n in m.graph.node:
    if n.op_type == "HardSwish":
        t = f"{n.output[0]}__hardsigmoid"
        nodes.append(helper.make_node("HardSigmoid", [n.input[0]], [t], name=f"{n.name}__hs", alpha=1/6, beta=0.5))
        nodes.append(helper.make_node("Mul", [n.input[0], t], [n.output[0]], name=f"{n.name}__mul"))
    else:
        nodes.append(n)
del m.graph.node[:]
m.graph.node.extend(nodes)
onnx.save(m, "en_PP-OCRv5_rec_mobile.onnx")
```
