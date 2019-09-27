package com.sonant.paymentcollection.Payment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sonant.paymentcollection.R;

public class CardScanActivity extends AppCompatActivity {

    TextInputEditText inputEditText;
    Button nextButton;
    DatabaseReference databaseReference;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_scan);

        inputEditText = findViewById(R.id.cardNoET);
        nextButton = findViewById(R.id.nextButton);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading...");
        progressDialog.create();

        inputEditText.setText("0062277799");

        databaseReference = FirebaseDatabase.getInstance("https://dtdnavigator.firebaseio.com/").getReference();

        nextButton.setOnClickListener(view -> checkCard());

    }

    private void checkCard() {
        String cardUID = inputEditText.getText().toString().trim();
        if (cardUID.length() != 10) {
            inputEditText.setError("Enter 10 digits");
            inputEditText.requestFocus();
            return;
        }
        showProgressDialog();
        databaseReference.child("CardScanData").child(cardUID).child("SerialNo")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() == null) {
                            dismissProgressDialog();
                            inputEditText.setError("Wrong Card Number");
                            inputEditText.requestFocus();
                            return;
                        }
                        Intent intent = new Intent(CardScanActivity.this, PaymentActivity.class);
                        intent.putExtra("serialNo", dataSnapshot.getValue().toString());

                        progressDialog.dismiss();

                        startActivity(intent);

                        finish();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void showProgressDialog() {
        if (progressDialog != null) {
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && !isFinishing()) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }
}
