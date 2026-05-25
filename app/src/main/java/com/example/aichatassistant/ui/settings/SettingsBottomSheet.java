package com.example.aichatassistant.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.aichatassistant.R;
import com.example.aichatassistant.common.AppConfig;
import com.example.aichatassistant.di.ServiceLocator;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Settings bottom sheet.
 *
 * Allows runtime override of the system prompt.
 * API key and base URL are shown as read-only info here;
 * in a production app they would be stored in EncryptedSharedPreferences
 * and surfaced through a proper settings Activity.
 */
public class SettingsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "SettingsBottomSheet";

    public static SettingsBottomSheet newInstance() {
        return new SettingsBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText etSystemPrompt = view.findViewById(R.id.et_system_prompt);
        TextInputEditText etApiKey       = view.findViewById(R.id.et_api_key);
        TextView          tvModel        = view.findViewById(R.id.tv_model_value);
        SwitchMaterial    swMock         = view.findViewById(R.id.sw_mock_provider);
        Button            btnSave        = view.findViewById(R.id.btn_save_settings);

        // Populate with current values
        etSystemPrompt.setText(ServiceLocator.get().getPromptBuilder().getSystemPrompt());
        etApiKey.setHint("API key hidden for security");
        tvModel.setText(AppConfig.DEFAULT_MODEL);
        swMock.setChecked(AppConfig.USE_MOCK_PROVIDER);

        // USE_MOCK_PROVIDER is a compile-time constant; toggling requires rebuild.
        // Disable the switch to make this clear to the user.
        swMock.setEnabled(false);
        swMock.setAlpha(0.5f);

        btnSave.setOnClickListener(v -> {
            android.text.Editable promptEditable = etSystemPrompt.getText();
            String newPrompt = promptEditable != null ? promptEditable.toString().trim() : "";
            if (!newPrompt.isEmpty()) {
                ServiceLocator.get().getPromptBuilder().setSystemPrompt(newPrompt);
            }
            dismiss();
        });
    }
}
