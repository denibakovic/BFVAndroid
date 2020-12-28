package com.bfv.BFVAndroid.fragments.parameters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bfv.BFVAndroid.R;
import com.bfv.BFVAndroid.SharedDataViewModel;
import com.bfv.BFVAndroid.bluetooth.BluetoothController;
import com.bfv.BFVAndroid.bluetooth.BluetoothProvider;

import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import BFVLib.BFV;
import BFVLib.Command;


public class ParametersFragment extends Fragment implements ParametersRecyclerAdapter.ItemClickListener {

    private Map<String, Command> parameters;
    private BluetoothController bluetoothController;
    private ParametersRecyclerAdapter parametersRecyclerAdapter;
    private SharedDataViewModel sharedData;
    private RecyclerView parametersRecyclerView;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // sharedData
        sharedData = new ViewModelProvider(getActivity()).get(SharedDataViewModel.class);
        parameters = sharedData.getBfv().getAllParameters();

        // Views
        View rootView = inflater.inflate(R.layout.fragment_parameters, container, false);
        parametersRecyclerView = rootView.findViewById(R.id.parametersRecyclerView);

        // Set up the RecyclerView layout manager
        parametersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        parametersRecyclerAdapter = new ParametersRecyclerAdapter(getContext(), parameters, bluetoothController);
        parametersRecyclerAdapter.setClickListener(this);
        parametersRecyclerView.setAdapter(parametersRecyclerAdapter);

        DividerItemDecoration itemDecorator = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.divider));

        parametersRecyclerView.addItemDecoration(itemDecorator);

        /*
         * HACK: this actually works for updating values, but is should probably be implemented
         *  differently
         *
         * sharedData.getBfv().getAllParameters() gets passed to ParametersRecyclerAdapter via parameters
         * variable so it doesn't update currently shown items unless user scrolls them off screen
         * and back on(its not aware of changes until it tries to access the values).
         *
         * This calls parametersRecyclerAdapter.notifyDataSetChanged() every second which is short
         * enough for user experience and long enough so it doesn't interfere with onItemClick
         */
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                parametersRecyclerView.post(() -> parametersRecyclerAdapter.notifyDataSetChanged());
            }
        }, 0, 1000);

        return rootView;
    }


    @Override
    public void onParameterItemClick(int position) {
        String parameterName = (String) parameters.keySet().toArray()[position];
        Command parameter = parameters.get(parameterName);

        EditText editParameter = new EditText(getContext());
        editParameter.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editParameter.setRawInputType(Configuration.KEYBOARD_12KEY);

        editParameter.setHint("Insert value");

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext())
                .setTitle(parameterName)
                .setNegativeButton("Close", (dialog, whichButton) -> {});

        switch (parameter.getType()) {
            case BFV.TYPE_BOOLEAN:
                buildDialog(parameter, editParameter, dialogBuilder,
                        parameter.getDefaultValue(), parameter.getMinVal(), parameter.getDefaultValue(),
                        true);
                break;

            case BFV.TYPE_INT:
                buildDialog(parameter, editParameter, dialogBuilder,
                        parameter.getMinVal(), parameter.getMaxVal(), parameter.getDefaultValue(),
                        false);
                break;

            case BFV.TYPE_DOUBLE:
                buildDialog(parameter, editParameter, dialogBuilder,
                        parameter.getMinVal() / parameter.getFactor(),
                        parameter.getMaxVal() / parameter.getFactor(),
                        parameter.getDefaultValue() / parameter.getFactor(),
                        false);
                break;

            case BFV.TYPE_INTOFFSET:
                buildDialog(parameter, editParameter, dialogBuilder,
                        parameter.getMinVal() + parameter.getFactor(),
                        parameter.getMaxVal() + parameter.getFactor(),
                        parameter.getDefaultValue() + parameter.getFactor(),
                        false);
                break;
        }

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        if(parameter.getType() != BFV.TYPE_BOOLEAN) {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }

        editParameter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(s.length() > 0);
            }
        });
    }


    private void buildDialog(Command parameter, EditText editParameter, AlertDialog.Builder dialogBuilder, double minVal, double maxVal, double defaultValue, boolean isBoolType) {
        if (bluetoothController.getState() == BluetoothProvider.STATE_CONNECTED) {
            if(isBoolType) {
                final ListView choicesListView = getBooleanListView(parameter, dialogBuilder);
                dialogBuilder.setPositiveButton("Update", getOnClickListenerBool(parameter, choicesListView));
            }
            else {
                dialogBuilder.setView(editParameter)
                        .setMessage(String.format("Min: %s  Max: %s  Def: %s",
                                minVal, maxVal, defaultValue))
                        .setPositiveButton("Update",
                                getOnClickListener(parameter, editParameter));
            }
        }
        else {
            editParameter.setHint("Connect to device to change values!");
            editParameter.setEnabled(false);
            dialogBuilder.setView(editParameter);

            if(isBoolType) {
                dialogBuilder.setMessage(String.format("TRUE or FALSE Def: %S",
                        (defaultValue == 1)));
            }
            else {
                dialogBuilder.setMessage(String.format("Min: %s  Max: %s  Def: %s",
                        minVal, maxVal, defaultValue));
            }
        }
    }


    private ListView getBooleanListView(Command parameter, AlertDialog.Builder dialogBuilder) {
        final ArrayAdapter<String> arrayAdapterItems = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_list_item_single_choice, Arrays.asList("False", "True"));

        View content = getLayoutInflater().inflate(R.layout.parameters_boolean_dialog, null);

        TextView descriptionTextView = content.findViewById(R.id.parameterDescriptionTextView);
        descriptionTextView.setText(String.format("Input Value - Default: %S", (parameter.getDefaultValueAsString())));

        final ListView choicesListView = content.findViewById(R.id.booleanChoiceListView);
        choicesListView.setAdapter(arrayAdapterItems);
        choicesListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


        dialogBuilder.setView(content);
        return choicesListView;
    }


    private DialogInterface.OnClickListener getOnClickListenerBool(Command parameter, ListView choicesListView) {
        return (dialog, which) -> {
            int checked = choicesListView.getCheckedItemPosition();
            try {
                if( ! sharedData.getDryRun().getValue()) {
                    if (parameter.setValue(checked)) {
                        bluetoothController.writeToBT(parameter.serializeCommand());
                        getSettings();
                        Toast.makeText(getContext(), "Value sent!", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getContext(), "Bad input value!", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(getContext(), "Dry Run is ON, no command sent!", Toast.LENGTH_LONG).show();
                    Log.i("createDialogBoolean: ", "Send command: " + parameter.serializeCommand());
                }
            } catch (NumberFormatException | NullPointerException ne) {
                //textField.setBackground(ParameterField.colorValueWrong);
                Toast.makeText(getContext(), "Bad input value!", Toast.LENGTH_LONG).show();
            }
        };
    }


    private DialogInterface.OnClickListener getOnClickListener(Command parameter, EditText editParameter) {
        return (dialog, whichButton) -> {
            try {
                if( ! sharedData.getDryRun().getValue()) {
                    if (parameter.setValue(editParameter.getText().toString())) {
                        bluetoothController.writeToBT(parameter.serializeCommand());
                        getSettings();
                        Toast.makeText(getContext(), "Value sent!", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getContext(), "Bad input value!", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(getContext(), "Dry Run is ON, no command sent!", Toast.LENGTH_LONG).show();
                    Log.i("createDialog: ", "Send command: " + parameter.serializeCommand());
                }
            } catch (NumberFormatException | NullPointerException ne) {
                //textField.setBackground(ParameterField.colorValueWrong);
                Toast.makeText(getContext(), "Bad input value!", Toast.LENGTH_LONG).show();
            }
        };
    }


    @Override
    public void onAttach(@NonNull Context context) {
        // We use bluetoothController to command BluetoothProvider via MainActivity that implements
        // BluetoothController interface
        bluetoothController = (BluetoothController) context;
        super.onAttach(context);
    }


    private void getSettings() {
        bluetoothController.writeToBT(sharedData.getBfv().getAllCommands().get("getSettings").serializeCommand());
    }
}
