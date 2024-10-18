package ca.sample.foodrecipeapp.ui.favorites;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ca.sample.foodrecipeapp.FoodData;
import ca.sample.foodrecipeapp.LoginActivity;
import ca.sample.foodrecipeapp.MyAdapter;
import ca.sample.foodrecipeapp.R;

public class FavoritesFragment extends Fragment {

    private FavoritesViewModel favoritesViewModel;
    RecyclerView mRecyclerView;
    List<FoodData> myFoodList;
    TextView txt_empty; // TextView for displaying the empty message
    ProgressDialog progressDialog;
    EditText txt_search;
    MyAdapter myAdapter;
    String userId;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        favoritesViewModel = ViewModelProviders.of(this).get(FavoritesViewModel.class);
        View root = inflater.inflate(R.layout.fragment_favorites, container, false);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mRecyclerView = root.findViewById(R.id.recyclerView);
        txt_empty = root.findViewById(R.id.txt_empty); // Initialize the empty message TextView
        GridLayoutManager gridLayoutManager = new GridLayoutManager(root.getContext(), 1);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        txt_search = root.findViewById(R.id.txt_searchtext);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            Intent intent = new Intent(getContext(), LoginActivity.class);
            String warning = "You need to Login First to unlock this feature!!";
            intent.putExtra("warning", warning);
            startActivity(intent);
        } else {
            userId = firebaseAuth.getCurrentUser().getUid();

            progressDialog = new ProgressDialog(root.getContext());
            progressDialog.setMessage("Loading Recipes...");

            myFoodList = new ArrayList<>();

            progressDialog.show();

            final DocumentReference documentReference = firebaseFirestore.collection("users").document(userId);
            documentReference.collection("Favorites").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                    myFoodList.clear();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        FoodData foodData = documentSnapshot.toObject(FoodData.class);
                        foodData.setKey(foodData.getKey());
                        foodData.setRecipeKey(documentSnapshot.getId());
                        myFoodList.add(foodData);
                    }
                    myAdapter.notifyDataSetChanged();
                    progressDialog.dismiss();

                    // Check if the list is empty and show the appropriate message
                    if (myFoodList.isEmpty()) {
                        txt_empty.setVisibility(View.VISIBLE);
                        txt_empty.setText("No favorite recipes added yet!");
                    } else {
                        txt_empty.setVisibility(View.GONE);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                }
            });

            txt_search.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

                @Override
                public void afterTextChanged(Editable editable) {
                    filter(editable.toString());
                }
            });

            myAdapter = new MyAdapter(root.getContext(), myFoodList);
            mRecyclerView.setAdapter(myAdapter);
        }
        return root;
    }

    private void filter(String text) {
        ArrayList<FoodData> filterList = new ArrayList<>();

        for (FoodData item : myFoodList) {
            if (item.getItemName().toLowerCase().contains(text.toLowerCase())) {
                filterList.add(item);
            }
        }

        myAdapter.filteredList(filterList);

        // Check if the filtered list is empty and update the empty message
        if (filterList.isEmpty()) {
            txt_empty.setVisibility(View.VISIBLE);
            txt_empty.setText("No favorite recipes added yet!");
        } else {
            txt_empty.setVisibility(View.GONE);
        }
    }
}
