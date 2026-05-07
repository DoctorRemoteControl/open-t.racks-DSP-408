# Protocol Spec Status

`DspLib-408.json` and `DspLib-408-fir.json` are the canonical DSP408Controller protocol and field-library standards.

Canonical files:

* `docs/protocol-spec/DspLib-408.json`
* `docs/protocol-spec/DspLib-408-fir.json`

Status of the files in this folder:

* `DspLib-408.json` is the common DSP408 protocol library and the baseline for shared features.
* `DspLib-408-fir.json` has the same shared format plus FIR408-only features.

For new implementation work, keep shared DSP408/FIR408 features compatible between both files. Put FIR-only commands and fields only in `DspLib-408-fir.json`.
