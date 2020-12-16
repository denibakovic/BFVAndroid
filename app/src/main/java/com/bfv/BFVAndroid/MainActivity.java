package com.bfv.BFVAndroid;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.bfv.BFVAndroid.bluetooth.BluetoothController;
import com.bfv.BFVAndroid.bluetooth.BluetoothProvider;
import com.bfv.BFVAndroid.bluetooth.BluetoothAplication;
import com.bfv.BFVAndroid.fragments.dashboard.DashboardFragment;
import com.bfv.BFVAndroid.fragments.devices.DevicesFragment;
import com.bfv.BFVAndroid.fragments.parameters.ParametersFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.TreeMap;

import BFVlib.Command;


/**
 * Main and only activity in this app
 * Binds to BluetoothProvider and creates sharedData for Fragments and BluetoothSerialThread to use
 */
public class MainActivity extends AppCompatActivity implements BluetoothController, SendCommandRecyclerAdapter.ItemClickListener {

    private static final int TAB_DEVICES = 0;
    private static final int TAB_DASHBOARD = 1;
    private static final int TAB_PARAMETERS = 2;

    private BluetoothProvider bluetoothProvider;
    private SharedDataViewModel sharedData;
    private MenuItem sendCommand;
    private TreeMap<String, Command> commands;


    // TODO: add parameters save / reload / share
    // MAYBE: mark commands that are made for higher HW version different color
    // MAYBE: add connection status(or icon) connected / disconnected to status bar
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a ViewModel the first time the system calls an activity's onCreate() method.
        // Re-created activities receive the same SharedDataViewModel instance created by the first activity.
        sharedData = new ViewModelProvider(this).get(SharedDataViewModel.class);

        // Bluetooth application provides all bluetooth related methods
        // via bluetooth controller interface
        BluetoothAplication bluetoothAplication = (BluetoothAplication) getApplication();
        bluetoothProvider = bluetoothAplication.getBluetoothProvider();
        bluetoothProvider.setSharedData(sharedData);

        setContentView(R.layout.activity_main);

        // ViewPager setup
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager viewPager = findViewById(R.id.viewPager);
        PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager());

        // Attaching fragments to adapter
        pagerAdapter.addFragment(new DevicesFragment(),"Devices");  // TAB_DEVICES
        pagerAdapter.addFragment(new DashboardFragment(),"Dashboard");  // TAB_DASHBOARD
        pagerAdapter.addFragment(new ParametersFragment(),"Parameters");  // TAB_PARAMETERS

        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);

        // Setting icons
        tabLayout.getTabAt(TAB_DEVICES).setIcon(R.drawable.ic_devices_24);
        tabLayout.getTabAt(TAB_DASHBOARD).setIcon(R.drawable.ic_dashboard_black_24dp);
        tabLayout.getTabAt(TAB_PARAMETERS).setIcon(R.drawable.ic_parameters_24);

        viewPager.setCurrentItem(TAB_DASHBOARD);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        sendCommand = menu.findItem(R.id.settings_sendCommand);
        return true;
    }


    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        sendCommand.setEnabled(getState() == BluetoothProvider.STATE_CONNECTED);

        return super.onMenuOpened(featureId, menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings_dry_run:
                if(item.isChecked()) {
                    Toast.makeText(this, "Dry run OFF", Toast.LENGTH_SHORT).show();
                    sharedData.setDryRun(false);
                    item.setChecked(false);
                } else {
                    Toast.makeText(this, "Dry run ON", Toast.LENGTH_SHORT).show();
                    sharedData.setDryRun(true);
                    item.setChecked(true);
                }
                return true;

            case R.id.settings_about:
                showAboutDialog();
                return true;

            case R.id.settings_sendCommand:
                showSendCommandDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onSendCommandItemClick(View view, int position) {
        String commandName = (String) commands.keySet().toArray()[position];
        Command command = commands.get(commandName);

        if(command.acceptsArguments()) {
            android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(this)
                    .setTitle(commandName)
                    .setNegativeButton("Cancel", (dialog, whichButton) -> {});

            EditText editParameter = new EditText(this);
            editParameter.setHint(command.getDefaultArguments());

            dialogBuilder
                    .setView(editParameter)
                    .setPositiveButton("Send", (dialog, whichButton) -> {
                        String editedValue = editParameter.getText().toString();
                        if (editedValue.isEmpty()) {
                            Toast.makeText(this, "Insert value before sending!", Toast.LENGTH_LONG).show();
                        }
                        else {
                            if( ! sharedData.getDryRun().getValue()) {
                                if (bluetoothProvider.write(command.serializeCommand(editedValue))) {
                                    Toast.makeText(this, "Sent " + commandName + " with value " + editedValue, Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    Toast.makeText(this, "Error sending " + commandName + " with value " + editedValue, Toast.LENGTH_LONG).show();
                                }
                            }
                            else {
                                Toast.makeText(this, "Dry Run is ON, no command sent!", Toast.LENGTH_LONG).show();
                                Log.i("onSendCommandClick: ", "Send command: " + command.serializeCommand());
                            }
                        }
                    });
            dialogBuilder.create().show();
        } else {
            if( ! sharedData.getDryRun().getValue()) {
                android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(this)
                        .setTitle("Send " + commandName + "?")
                        .setNegativeButton("Cancel", (dialog, whichButton) -> {});

                EditText editParameter = new EditText(this);
                editParameter.setHint("Confirm sending command!");
                editParameter.setEnabled(false);

                dialogBuilder
                        //.setView(editParameter)
                        .setPositiveButton("Send", (dialog, whichButton) -> {
                            if (bluetoothProvider.write(command.serializeCommand())) {
                                Toast.makeText(this, "Sent " + commandName, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Error sending " + commandName, Toast.LENGTH_LONG).show();
                            }
                        });
                dialogBuilder.create().show();
            }
            else {
                Toast.makeText(this, "Dry Run is ON, no command sent!", Toast.LENGTH_LONG).show();
                Log.i("onSendCommandClick: ", "Send command: " + command.serializeCommand());
            }
        }
    }


    @Override
    public void connectBtDevice(BluetoothDevice bd) {
        bluetoothProvider.connect(bd);
    }

    @Override
    public void disconnectBtDevice() {
        bluetoothProvider.disconnect();
    }

    @Override
    public boolean writeToBT(String data) {
        return bluetoothProvider.write(data);
    }

    @Override
    public int getState() {
        return bluetoothProvider.getState();
    }

    @Override
    public BluetoothDevice getConnectedDevice() {
        return bluetoothProvider.getConnectedDevice();
    }

    @Override
    public BluetoothDevice getPreviousConnectedDevice() {
        return bluetoothProvider.getPreviousConnectedDevice();
    }


    /**
     * Displays About Dialog when called
     */
    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About");
        builder.setView(R.layout.about_dialog);
        builder.setNegativeButton("Close", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.show();
    }


    /**
     * Displays SendCommand Dialog when called
     */
    private void showSendCommandDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View convertView = getLayoutInflater().inflate(R.layout.send_command_dialog, null);

        RecyclerView commandsRecyclerView = convertView.findViewById(R.id.sendCommandRecyclerView);
        commandsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        commands = new TreeMap<>(sharedData.bfv.getAllCommands());
        SendCommandRecyclerAdapter adapter = new SendCommandRecyclerAdapter(this, commands);
        adapter.setClickListener(this);
        commandsRecyclerView.setAdapter(adapter);

        builder.setTitle("Send Command");
        builder.setView(convertView);
        builder.setNegativeButton("Close", (dialogInterface, i) -> dialogInterface.dismiss());

        builder.create().show();
    }
}