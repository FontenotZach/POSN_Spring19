package com.posn.asynctasks;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;

import com.posn.Constants;
import com.posn.exceptions.POSNCryptoException;
import com.posn.main.LoginActivity;
import com.posn.main.MainActivity;
import com.posn.utility.DeviceFileManager;
import com.posn.utility.POSNDataManager;
import com.posn.utility.SymmetricKeyManager;
import com.posn.utility.UserInterfaceManager;

import org.json.JSONException;

import java.io.IOException;


public class AuthenticateUserAsyncTask extends AsyncTask<String, String, String>
   {
      private ProgressDialog pDialog;
      private String deviceFileKey;
      private String email, password;
      private boolean loginVerify;
      private LoginActivity activity;

      POSNDataManager dataManager;


      public AuthenticateUserAsyncTask(LoginActivity activity, String email, String password)
         {
            super();
            this.email = email;
            this.password = password;
            this.activity = activity;
            loginVerify = false;
         }

      @Override
      protected void onPreExecute()
         {
            super.onPreExecute();
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Authenticating...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
         }

      protected String doInBackground(String... params)
         {
            try
               {
                  // check if password is valid
                  deviceFileKey = SymmetricKeyManager.createKeyFromString(password);

                  String verificationString = DeviceFileManager.loadStringFromFile(Constants.encryptionKeyFilePath, "verify.pass");

                  verificationString = SymmetricKeyManager.decrypt(deviceFileKey, verificationString);

                  // verify the password
                  if (verificationString != null)
                     {
                        if (verificationString.equals("POSN - SUCCESS"))
                           {
                              // create a new data manager object
                              dataManager = new POSNDataManager(deviceFileKey);

                              // load the user application file from the device
                              dataManager.loadUserAppFile();

                              // verify email address
                              if (email.equals(dataManager.user.email))
                                 {
                                    loginVerify = true;
                                 }
                           }
                     }
               }
            catch (POSNCryptoException | JSONException | IOException error)
               {
                  error.printStackTrace();
               }

            return null;
         }

      protected void onPostExecute(String file_url)
         {
            // dismiss the dialog once done
            pDialog.dismiss();

            if (loginVerify)
               {
                  Intent intent = new Intent(activity, MainActivity.class);
                  intent.putExtra("dataManager", dataManager);

                  if (activity.uri != null)
                     {
                        intent.putExtra("uri", activity.uri.toString());
                     }

                  // Close all views before launching Employer
                  activity.startActivity(intent);
                  activity.finish();

               }
            else
               {
                  // clear the email and password edit text
                  activity.emailText.getText().clear();
                  activity.passwordText.getText().clear();

                  // display an error
                  UserInterfaceManager.showToast(activity, "Invalid email address or password. Please log in again.");
               }
         }

   }