package ca.sample.foodrecipeapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class DetailActivity extends AppCompatActivity {

    TextView foodDescription, recipeName, recipePrice;
    ImageView foodImage, like_imageView, dislike_imageView;
    Button btnUpdate, btnDelete;
    String recipeItemName, recipeItemPrice, recipeDescription;
    String key = "";
    String recipeKey = "";
    String imageUrl = "";
    String favoriteKey = "";
    String userID;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        recipeName = findViewById(R.id.txtRecipeName);
        recipePrice = findViewById(R.id.txtPrice);
        foodDescription = findViewById(R.id.txtDescription);
        foodImage = findViewById(R.id.ivImage2);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);
        like_imageView = findViewById(R.id.like_imageView);
        dislike_imageView = findViewById(R.id.dislike_imageView); // Corrected line

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            Intent intent = new Intent(this, LoginActivity.class);
            String warning = "You need to Login First to unlock this feature!!";
            intent.putExtra("warning", warning);
            startActivity(intent);
        } else {
            userID = firebaseAuth.getCurrentUser().getUid();
            Bundle mBundle = getIntent().getExtras();

            if (mBundle != null) {
                recipeItemName = mBundle.getString("RecipeName");
                recipeName.setText(recipeItemName);
                recipeItemPrice = mBundle.getString("price");
                recipePrice.setText(recipeItemPrice);
                recipeDescription = mBundle.getString("Description");
                foodDescription.setText(recipeDescription);
                key = mBundle.getString("keyValue");
                recipeKey = mBundle.getString("recipeKeyValue");
                imageUrl = mBundle.getString("Image");
                Glide.with(this).load(imageUrl).into(foodImage);
            }

            Log.d("TAG DetailActivity", "Recipe Key: " + recipeKey);
            checkIfRecipeIsFavorite();
            checkIfUserCanUpdateOrDelete();
        }
    }

    private void checkIfRecipeIsFavorite() {
        DocumentReference favoriteDocumentReference = firebaseFirestore.collection("users").document(userID);
        favoriteDocumentReference.collection("Favorites").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    FoodData foodData = documentSnapshot.toObject(FoodData.class);
                    if (key.equals(foodData.getKey())) {
                        like_imageView.setVisibility(View.VISIBLE);
                        dislike_imageView.setVisibility(View.GONE);
                        favoriteKey = documentSnapshot.getId();
                        Log.d("TAG", "favoriteKey: " + favoriteKey);
                    }
                }
            }
        });
    }

    private void checkIfUserCanUpdateOrDelete() {
        if (recipeKey != null) {
            DocumentReference documentReference = firebaseFirestore.collection("users").document(userID).collection("Recipes").document(recipeKey);
            documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                    if (error != null) {
                        Log.d("TAG", "onEvent: " + error.getMessage());
                    } else {
                        String compareKey = value.getString("key");
                        if (key.equals(compareKey)) {
                            btnUpdate.setVisibility(View.VISIBLE);
                            btnDelete.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
    }

    public void btnDeleteRecipe(View view) {
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Recipe");
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReferenceFromUrl(imageUrl);

        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                reference.child(key).removeValue();
                deleteRecipeFromFirestore();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("TAG", "Failed to delete image: " + e.getMessage());
                Snackbar.make(findViewById(R.id.txtRecipeName), "Failed to Delete Image: " + e.getMessage(), BaseTransientBottomBar.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteRecipeFromFirestore() {
        DocumentReference documentReference = firebaseFirestore.collection("users").document(userID).collection("Recipes").document(recipeKey);
        documentReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("TAG", "Recipe deleted from Firestore");
                        Snackbar.make(findViewById(R.id.txtRecipeName), "Recipe Deleted", BaseTransientBottomBar.LENGTH_SHORT).show();
                        deleteFavoriteRecipe();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("TAG", "Recipe not deleted from Firestore: " + e.getMessage());
                        Snackbar.make(findViewById(R.id.txtRecipeName), "Failed to Delete Recipe: " + e.getMessage(), BaseTransientBottomBar.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteFavoriteRecipe() {
        DocumentReference documentReferenceFav = firebaseFirestore.collection("users").document(userID).collection("Favorites").document(favoriteKey);
        documentReferenceFav.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("TAG", "Favorite recipe deleted from Firestore");
                        Snackbar.make(findViewById(R.id.txtRecipeName), "Recipe Deleted from Favorites", BaseTransientBottomBar.LENGTH_SHORT).show();
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("TAG", "Favorite recipe not deleted from Firestore: " + e.getMessage());
                        Snackbar.make(findViewById(R.id.txtRecipeName), "Failed to Delete Recipe from Favorites: " + e.getMessage(), BaseTransientBottomBar.LENGTH_SHORT).show();
                    }
                });
    }

    public void btnUpdateRecipe(View view) {
        startActivity(new Intent(getApplicationContext(), UpdateRecipeActivity.class)
                .putExtra("recipeNameKey", recipeName.getText().toString())
                .putExtra("descriptionKey", foodDescription.getText().toString())
                .putExtra("priceKey", recipePrice.getText().toString())
                .putExtra("oldImageUrl", imageUrl)
                .putExtra("key", key)
                .putExtra("recipeKey", recipeKey));
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void btnLike(View view) {
        DocumentReference documentReference = firebaseFirestore.collection("users").document(userID).collection("Favorites").document(favoriteKey);
        documentReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("TAG", "Recipe removed from Favorites");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("TAG", "Failed to remove recipe from Favorites: " + e.getMessage());
                        Snackbar.make(findViewById(R.id.txtRecipeName), "Failed to Remove Recipe: " + e.getMessage(), BaseTransientBottomBar.LENGTH_SHORT).show();
                    }
                });

        dislike_imageView.setVisibility(View.VISIBLE);
        like_imageView.setVisibility(View.GONE);
    }

    public void btnDislike(final View view) {
        favoriteKey = firebaseFirestore.collection("users").document(userID).collection("Favorites").document().getId();
        FoodData fireStoreFoodData = new FoodData(recipeItemName, recipeDescription, recipeItemPrice, imageUrl, key, favoriteKey);

        DocumentReference documentReference = firebaseFirestore.collection("users").document(userID).collection("Favorites").document(favoriteKey);
        documentReference.set(fireStoreFoodData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("TAG", "Recipe added to Favorites");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("TAG", "Failed to add recipe to Favorites: " + e.getMessage());
                        Snackbar.make(findViewById(R.id.txtRecipeName), "Failed to Add Recipe: " + e.getMessage(), BaseTransientBottomBar.LENGTH_SHORT).show();
                    }
                });

        like_imageView.setVisibility(View.VISIBLE);
        dislike_imageView.setVisibility(View.GONE);
    }
}
