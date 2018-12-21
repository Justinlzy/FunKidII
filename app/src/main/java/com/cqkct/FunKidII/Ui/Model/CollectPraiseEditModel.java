package com.cqkct.FunKidII.Ui.Model;

import com.cqkct.FunKidII.Ui.Activity.MoreFunction.CollectPraiseEditActivity;

import java.lang.ref.WeakReference;

public class CollectPraiseEditModel {
    private WeakReference<CollectPraiseEditActivity> a;

    CollectPraiseEditModel(CollectPraiseEditActivity activity) {
        a = new WeakReference<>(activity);
    }

}
