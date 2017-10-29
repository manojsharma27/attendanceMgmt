package com.ms.app.attendancemgmt.activitiy;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.model.Employee;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.StringUtils;

public class RegisterAttendanceActivity extends AppCompatActivity {

    private TextView tvEmpName;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_register_attendance);

        String pin = this.getIntent().getExtras().getString(Constants.EMP_PIN);
        final Employee employee = Utility.searchEmployeeFromPin(pin);
        if (null == employee) {
            showEmpNotFoundDialog();
            finish();
            return;
        }

        tvEmpName = (TextView) findViewById(R.id.tvEmpName);
        tvEmpName.setText(String.format(Constants.HELLO_NAME, employee.getName()));

        Button btnRegAttendance = (Button) findViewById(R.id.btnRegisterAttendance);
        btnRegAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerAttendance(employee);
            }
        });
    }

    private void populateToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showEmpNotFoundDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error:");
        builder.setMessage("Oops! Your details not found.");
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.show();
    }

    @Override
    public void finish() {
        super.finish();
    }

    private void registerAttendance(Employee emp) {
        populateToast("Registered attendance for " + emp.getName());

        /*
        get location,
        get timestamp in utc,
        get empId,

         */
    }
}
