package com.wally.wally.controllers.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.wally.wally.R;
import com.wally.wally.Utils;
import com.wally.wally.adf.AdfInfo;

/**
 * This is dialog that manages ADF import export permission grant and returns callback statuses via
 * {@link ImportExportPermissionListener} interface.
 * This dialog also helps to manage explanation messages.
 * <p/>
 * Note that if your activity doesn't implement {@link ImportExportPermissionListener} than you will get exception.
 * Created by ioane5 on 7/8/16.
 */
public class ImportExportPermissionDialogFragment extends DialogFragment implements View.OnClickListener {

    public static final String TAG = ImportExportPermissionDialogFragment.class.getSimpleName();
    private static final int IMPORT = 0;
    public static final int EXPORT = 1;
    private static final String ARG_ADF_INFO = "ARG_ADF_INFO";
    private static final String INTENT_CLASSPACKAGE = "com.projecttango.tango";
    private static final String INTENT_IMPORTEXPORT_CLASSNAME = "com.google.atap.tango.RequestImportExportActivity";
    private static final String EXTRA_KEY_SOURCEFILE = "SOURCE_FILE";
    private static final String ARG_MODE = "ARG_MODE";

    private static final int REQ_CODE = 52;
    /**
     * Import or Export
     */
    private int mMode;
    private ImportExportPermissionListener mListener;

    private AdfInfo mAdfInfo;
    private boolean mIsShowingPermission = false;

    /**
     * @param adfInfo        adfInfo
     * @param importOrExport public integer that represents IMPORT/EXPORT enumeration.
     * @return Fragment that should be shown
     */
    public static ImportExportPermissionDialogFragment newInstance(AdfInfo adfInfo, int importOrExport) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_ADF_INFO, adfInfo);
        args.putInt(ARG_MODE, importOrExport);

        ImportExportPermissionDialogFragment fragment = new ImportExportPermissionDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        readArgs();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        @SuppressLint("InflateParams") View dv = LayoutInflater.from(getActivity())
                .inflate(R.layout.import_export_explain_dialog, null, false);

        initViews(dv);

        builder.setView(dv);
        Dialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mIsShowingPermission) {
            getDialog().hide();
        } else if (!shouldShowPermissionExplanation()) {
            getDialog().hide();
            requestPermission();
        }
    }

    private void requestPermission() {
        mIsShowingPermission = true;
        if (mMode == IMPORT) {
            requestImportPermission();
        } else {
            requestExportPermission();
        }
    }

    private void requestImportPermission() {
        Intent importIntent = new Intent();
        importIntent.setClassName(INTENT_CLASSPACKAGE, INTENT_IMPORTEXPORT_CLASSNAME);
        importIntent.putExtra(EXTRA_KEY_SOURCEFILE, Utils.getAdfFilePath(mAdfInfo.getUuid()));
        startActivityForResult(importIntent, REQ_CODE);
    }

    // This code is from Tango Internals! Not mine
    private void requestExportPermission() {
        Intent exportIntent = new Intent();
        exportIntent.setClassName("com.google.tango", "com.google.atap.tango.RequestImportExportActivity");
        if(exportIntent.resolveActivity(getActivity().getPackageManager()) == null) {
            exportIntent = new Intent();
            exportIntent.setClassName("com.projecttango.tango", "com.google.atap.tango.RequestImportExportActivity");
        }

        exportIntent.putExtra("SOURCE_UUID", mAdfInfo.getUuid());
        exportIntent.putExtra("DESTINATION_FILE",  Utils.getAdfFilesFolder());
        startActivityForResult(exportIntent, REQ_CODE);
    }

    private void readArgs() {
        Bundle b = getArguments();
        mMode = b.getInt(ARG_MODE);
        mAdfInfo = (AdfInfo) b.getSerializable(ARG_ADF_INFO);
    }

    private void initViews(View v) {
        TextView title = (TextView) v.findViewById(R.id.tv_title);
        TextView message = (TextView) v.findViewById(R.id.tv_message);
        v.findViewById(R.id.button_positive).setOnClickListener(this);
        if (mMode == IMPORT) {
            title.setText(R.string.adf_import_explain_title);
            message.setText(R.string.adf_import_explain_message);
        } else {
            title.setText(R.string.adf_export_explain_title);
            message.setText(R.string.adf_export_explain_message);
        }
    }

    private boolean shouldShowPermissionExplanation() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sp.getBoolean(getIsDeniedStatusKey(), true);
    }

    private String getIsDeniedStatusKey() {
        if (mMode == IMPORT) {
            return "IMPORT_DENY_STATUS";
        }
        return "EXPORT_DENY_STATUS";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE) {
            mIsShowingPermission = false;
            // Save deny status.
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor e = sp.edit();
            e.putBoolean(getIsDeniedStatusKey(), resultCode != Activity.RESULT_OK);
            e.apply();

            if (resultCode == Activity.RESULT_OK) {
                mListener.onPermissionGranted(mAdfInfo);
            } else {
                mListener.onPermissionDenied(mAdfInfo);
            }
            dismiss();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (ImportExportPermissionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ImportExportPermissionListener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("mIsShowingPermission",mIsShowingPermission);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mIsShowingPermission = savedInstanceState.getBoolean("mIsShowingPermission");
        }
    }

    // Got it button clicked
    @Override
    public void onClick(View view) {
        getDialog().hide();
        requestPermission();
    }

    public interface ImportExportPermissionListener {
        void onPermissionGranted(AdfInfo adfInfo);

        void onPermissionDenied(AdfInfo adfInfo);
    }
}
