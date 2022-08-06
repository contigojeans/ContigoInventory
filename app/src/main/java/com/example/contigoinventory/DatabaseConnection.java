package com.example.contigoinventory;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.User;

public class DatabaseConnection {
    public User getDatabaseConnection(Context context){
        App realmAppCon;
        String RealmAppId = "contigorealm-tsygt";
        Realm.init(context);
        realmAppCon =  new App(new AppConfiguration.Builder(RealmAppId).build());
        User user = realmAppCon.currentUser();
        //Credentials credentials = Credentials.emailPassword("contigojeans@gmail.com","contigo123!@#");
        realmAppCon.loginAsync(Credentials.anonymous(), new App.Callback<User>() {
            @Override
            public void onResult(App.Result<User> result) {
                if(result.isSuccess())
                {
                    Toast.makeText(context.getApplicationContext(),"Database Connection Successful!",Toast.LENGTH_LONG).show();
                    Log.v("MongoDB_CON","Logged In Successfully");
                }
                else
                {
                    Toast.makeText(context.getApplicationContext(),"Database Connection Failed",Toast.LENGTH_LONG).show();
                    System.out.println(result.getError());
                    Log.v("MongoDB_CON","Failed to Login");
                }
            }
        });
        return user;
    }
}
