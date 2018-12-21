package com.cqkct.FunKidII.Ui.Listener;

import com.google.protobuf.GeneratedMessageV3;

public interface OperateDataListener {
    void operateSuccess(GeneratedMessageV3 messageV3);

    void operateFailure(protocol.Message.ErrorCode errorCode);
}
