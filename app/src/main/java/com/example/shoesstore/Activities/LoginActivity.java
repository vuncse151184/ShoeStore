package com.example.shoesstore.Activities;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.shoesstore.Models.User;
import com.example.shoesstore.R;
import com.example.shoesstore.Util.CartDBHelper;
import com.example.shoesstore.Util.CartDetailDBHelper;
import com.example.shoesstore.Util.Database;
import com.example.shoesstore.Util.UserDBHelper;

public class LoginActivity extends AppCompatActivity {
    EditText txtUsername, txtPassword;
    SharedPreferences sharedPreferences;
    Database database;
    UserDBHelper userDBHelper;
    CartDBHelper cartDBHelper;
    CartDetailDBHelper cartDetailDBHelper;
    AlertDialog.Builder dialog;
    String username, password;

    private static final String CHANNEL_ID = "Shoes Store";

    private static int REQUEST_PERMISSION_CODE = 10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        database = new Database(this, "ShoesStore.sqlite", null, 1);
        dialog = new AlertDialog.Builder(this);

        userDBHelper = new UserDBHelper(database);
        cartDBHelper = new CartDBHelper(database);
        cartDetailDBHelper = new CartDetailDBHelper(database);
        userDBHelper.AddTable();

//        userDBHelper.deleteData();
        userDBHelper.addAdmin();

        txtPassword = findViewById(R.id.txtLoginPassword);
        txtUsername = findViewById(R.id.txtLoginEmail_Username);

        sharedPreferences = getSharedPreferences("ShoesStore", Context.MODE_PRIVATE);

        username = sharedPreferences.getString("username", "");
        password = sharedPreferences.getString("password", "");

        if (!username.isEmpty() || !password.isEmpty()) {
            User user = userDBHelper.checkLogin(username, password);
            if (user != null) {
                changeView(user);
            }
        }
    }

    public void btnLogin_Click(View view) {
        User user = userDBHelper.checkLogin(txtUsername.getText().toString(), txtPassword.getText().toString());
        if (user != null) {
            changeView(user);
        } else {
            loginFail();
        }
    }

    public void txtSignup_Click(View view) {
        Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
        startActivity(intent);
        finish();
    }

    private void changeView(User user) {
        Toast.makeText(this, "Login Sucess!!!", Toast.LENGTH_SHORT).show();
        if (username.isEmpty() || password.isEmpty()) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("userId", user.getUserId());
            editor.putString("username", user.getUsername());
            editor.putString("password", user.getPassword());
            editor.putInt("role", user.getRole());
            editor.apply();
        }

        if (user.getRole() == 2) {
            Cursor cartData = cartDBHelper.GetDatabaseByUserId(user.getUserId());
            if (cartData.moveToFirst()) {
                Cursor cartDetailData = cartDetailDBHelper.GetAllCartDetail(cartData.getInt(0));
                if (cartDetailData.moveToFirst()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        if (!notificationManager.areNotificationsEnabled()) {
                            Toast.makeText(this, "Not allowed", Toast.LENGTH_SHORT).show();
                            getPermission();
                        } else {
                            sendNotification(user, cartDetailData.getCount());
                            startUser();
                        }
                    } else {
                        startUser();
                    }
                } else {
                    startUser();
                }
            } else {
                startUser();
            }
        } else {
            startAdmin();
        }

    }

    private void startAdmin() {
        Intent intent = new Intent(LoginActivity.this, AdminViewActivity.class);
        startActivity(intent);
        finish();
    }

    private void startUser() {
        Intent intent = new Intent(LoginActivity.this, UserViewActivity.class);
        startActivity(intent);
        finish();
    }

    private void sendNotification(User user, int count) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, user.getUsername(), NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(notificationChannel);
        Notification notification = new Notification
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Hey " + user.getUsername() + ", you forgot something in your cart")
                .setContentText("You have " + count + " item(s) in your cart!!!!")
                .build();
        notificationManager.notify(user.getUserId(), notification);
    }

    private void getPermission() {
        String permissions = (Manifest.permission.POST_NOTIFICATIONS);
        requestPermissions(new String[]{permissions}, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {

            }
        }
    }

    private void loginFail() {
        dialog.setMessage("Login Error");
        dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
