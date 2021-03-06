package me.shouheng.omnilist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import java.lang.ref.WeakReference;

import me.shouheng.omnilist.PalmApp;
import me.shouheng.omnilist.R;
import me.shouheng.omnilist.manager.ModelHelper;
import me.shouheng.omnilist.model.enums.FeedbackType;
import me.shouheng.omnilist.model.tools.Feedback;
import me.shouheng.omnilist.model.tools.ModelFactory;
import me.shouheng.omnilist.utils.StringUtils;
import me.shouheng.omnilist.utils.ToastUtils;
import me.shouheng.omnilist.widget.WatcherTextView;

/**
 * Created by wangshouheng on 2017/12/3.*/
public class FeedbackDialog extends DialogFragment implements AdapterView.OnItemSelectedListener {

    private Feedback feedback;

    private OnSendClickListener onSendClickListener;

    private AppCompatEditText etEmail, etQuestion;

    public static FeedbackDialog newInstance(OnSendClickListener onSendClickListener) {
        Bundle args = new Bundle();
        FeedbackDialog fragment = new FeedbackDialog();
        fragment.setArguments(args);
        fragment.setOnSendClickListener(onSendClickListener);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_feedback_layout, null);

        feedback = feedback == null ? ModelFactory.getFeedback() : feedback;
        feedback.setFeedbackType(FeedbackType.ABRUPT_CRASH);

        etEmail = rootView.findViewById(R.id.et_email);
        TextInputLayout tilEmail = rootView.findViewById(R.id.til_email);
        etEmail.addTextChangedListener(new EmailFormatWatcher(tilEmail));
        etQuestion = rootView.findViewById(R.id.et_question);
        WatcherTextView wtQuestion = rootView.findViewById(R.id.wt_question);
        wtQuestion.bindEditText(etQuestion);
        AppCompatSpinner spFeedbackTypes = rootView.findViewById(R.id.sp_feedback_types);
        spFeedbackTypes.setOnItemSelectedListener(this);

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.feedback)
                .setView(rootView)
                .setPositiveButton(R.string.text_send, (dialog, which) -> {
                    if (!checkInput()) return;
                    if (onSendClickListener != null) onSendClickListener.onSend(FeedbackDialog.this, feedback);
                })
                .setNegativeButton(R.string.text_cancel, null)
                .create();
    }

    private void copyContentIfNecessary() {
        if (!TextUtils.isEmpty(etQuestion.getText().toString())) {
            ModelHelper.copyToClipboard(getActivity(), etQuestion.getText().toString());
            ToastUtils.makeToast(R.string.content_was_copied_to_clipboard);
        }
    }

    private boolean checkInput() {
        String email, details;
        if (TextUtils.isEmpty(email = etEmail.getText().toString())) {
            ToastUtils.makeToast(R.string.connect_email_required);
            copyContentIfNecessary();
            return false;
        }
        if (!StringUtils.validate(email)) {
            ToastUtils.makeToast(R.string.illegal_email_format);
            copyContentIfNecessary();
            return false;
        }
        feedback.setEmail(email);
        if (TextUtils.isEmpty(details = etQuestion.getText().toString())) {
            ToastUtils.makeToast(R.string.details_required);
            return false;
        }
        feedback.setQuestion(details);
        return true;
    }

    private static class EmailFormatWatcher implements TextWatcher {

        private WeakReference<TextInputLayout> weakEt;

        EmailFormatWatcher(TextInputLayout et) {
            this.weakEt = new WeakReference<>(et);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            TextInputLayout et = weakEt.get();
            if (StringUtils.validate(s.toString())) {
                if (et != null) et.setErrorEnabled(false);
                return;
            }
            if (et != null) {
                et.setErrorEnabled(true);
                et.setError(PalmApp.getContext().getString(R.string.illegal_email_format));
            }
        }
    }

    public void setOnSendClickListener(OnSendClickListener onSendClickListener) {
        this.onSendClickListener = onSendClickListener;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:feedback.setFeedbackType(FeedbackType.ABRUPT_CRASH);break;
            case 1:feedback.setFeedbackType(FeedbackType.FUNCTION_IMPROVEMENT);break;
            case 2:feedback.setFeedbackType(FeedbackType.FUNCTION_REQUIREMENT);break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    public interface OnSendClickListener {
        void onSend(FeedbackDialog dialog, Feedback feedback);
    }
}
