package com.tutk.P2PCam264.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;


public class ModifySecurityCodeDialog extends AlertDialog {

    protected ModifySecurityCodeDialog(Context context, String title) {
        super(context);

        LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mInflater.inflate(R.layout.modify_security_code, null);
        setView(view);
        setTitle(title);
    }
}
