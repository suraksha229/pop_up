package com.example.suraksha.popup;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.content.pm.PackageInstaller.Session;
import android.content.pm.PackageInstaller.SessionCallback;
import android.widget.Button;

import com.facebook.login.LoginClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class pop extends Activity {

    private static List<String> permissions;
    Session.StatusCallback statusCallback = new Session.StatusCallback();
    ProgressDialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.loginpop);

        DisplayMetrics dm= new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int) (width*0.8),(int) (height*0.6));

        Button button = (Button) findViewById(R.id.button);
        permissions = new ArrayList<String>();
        permissions.add("email");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Check if there is any Active Session, otherwise Open New
                // Session
                Session session = Session.getActiveSession();
                if (session == null) {
                    Session.openActiveSession(FbLoginActivity.this, true,
                            statusCallback);
                } else if (!session.isOpened()) {
                    session.openForRead(new Session.OpenRequest(
                            FbLoginActivity.this).setCallback(statusCallback)
                            .setPermissions(permissions));
                }
            }
        });
        Session session = Session.getActiveSession();
        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(this, null, statusCallback,
                        savedInstanceState);
            }
            if (session == null) {
                session = new Session(this);
            }
            Session.setActiveSession(session);
            session.addCallback(statusCallback);
            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
                session.openForRead(new Session.OpenRequest(this).setCallback(
                        statusCallback).setPermissions(permissions));
            }
        }

    }

    private class SessionStatusCallback implements Session.StatusCallback {

        @Override
        public void call(Session session, SessionState state,
                         Exception exception) {
            // Check if Session is Opened or not
            processSessionStatus(session, state, exception);
        }
    }

    public void processSessionStatus(Session session, SessionState state,
                                     Exception exception) {
        if (session != null && session.isOpened()) {
            if (session.getPermissions().contains("email")) {
                // Show Progress Dialog
                dialog = new ProgressDialog(FbLoginActivity.this);
                dialog.setMessage("Logging in..");
                dialog.show();
                LoginClient.Request.executeMeRequestAsync(session,
                        new Request.GraphUserCallback() {

                            @Override
                            public void onCompleted(GraphUser user,
                                                    Response response) {

                                if (dialog != null && dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                                if (user != null) {
                                    Map<String, Object> responseMap = new HashMap<String, Object>();
                                    GraphObject graphObject = response
                                            .getGraphObject();
                                    responseMap = graphObject.asMap();
                                    Log.i("FbLogin", "Response Map KeySet - "
                                            + responseMap.keySet());
                                    // TODO : Get Email
                                    // responseMap.get("email");
                                    String fb_id = user.getId();
                                    String email = null;
                                    String name = (String) responseMap
                                            .get("name");
                                    if (responseMap.get("email") != null) {
                                        email = responseMap.get("email")
                                                .toString();
                                        Intent i = new Intent(pop.this, pop.class);
                                        i.putExtra("Email", email);
                                        startActivity(i);
                                    } else {
                                        // Clear all session info & ask user to
                                        // login again
                                        Session session = Session
                                                .getActiveSession();
                                        if (session != null) {
                                            session.closeAndClearTokenInformation();
                                        }
                                    }
                                }
                            }
                        });
            } else {
                session.requestNewReadPermissions(new Session.NewPermissionsRequest(
                        FbLoginActivity.this, permissions));
            }
        }
    }

    /********** Activity Methods **********/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("FbLogin", "Result Code is - " + resultCode + "");
        Session.getActiveSession().onActivityResult(FbLoginActivity.this,
                requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Save current session
        super.onSaveInstanceState(outState);
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }

    @Override
    protected void onStart() {
        // TODO Add status callback
        super.onStart();
        Session.getActiveSession().addCallback(statusCallback);
    }

    @Override
    protected void onStop() {
        // TODO Remove callback
        super.onStop();
        Session.getActiveSession().removeCallback(statusCallback);
    }
}
