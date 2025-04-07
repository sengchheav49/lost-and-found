package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.lostandfound.activities.LoginActivity;
import com.example.lostandfound.activities.PostItemActivity;
import com.example.lostandfound.activities.ProfileActivity;
import com.example.lostandfound.adapters.ItemAdapter;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.utils.FirebaseUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyView;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigationView;
    private SearchView searchView;
    private Spinner categorySpinner;
    private Button btnPrevious;
    private Button btnNext;
    private TextView tvPageInfo;
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 4;
    private List<Item> allItems = new ArrayList<>();

    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseFirestore db;

    private ItemAdapter itemAdapter;
    private FirebaseAuth auth;
    private List<Item> itemList = new ArrayList<>();
    private String currentQuery = "";
    private String currentCategory = "";
    private int currentTabPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is logged in
        authListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        };

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = findViewById(R.id.tab_layout);
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        tvEmptyView = findViewById(R.id.tv_empty_view);
        progressBar = findViewById(R.id.progress_bar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        searchView = findViewById(R.id.search_view);
        categorySpinner = findViewById(R.id.category_spinner);
        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);
        tvPageInfo = findViewById(R.id.tv_page_info);

        // Initialize category spinner
        setupCategorySpinner();
        
        // Set up search view
        setupSearchView();

        // Initialize RecyclerView with animation
        int resId = R.anim.layout_animation_fall_down;
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(this, resId);
        recyclerView.setLayoutAnimation(animation);

        itemAdapter = new ItemAdapter(this, itemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(itemAdapter);
        
        // Set up pagination button listeners
        btnPrevious.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updatePageInfo();
                updatePaginatedList();
            }
        });

        btnNext.setOnClickListener(v -> {
            int maxPages = (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE);
            if (currentPage < maxPages) {
                currentPage++;
                updatePageInfo();
                updatePaginatedList();
            }
        });

        // Set up refresh listener
        swipeRefreshLayout.setOnRefreshListener(this::fetchItemsFromFirestore);

        // Set up tab listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                // Reset and refetch data when changing tabs
                fetchItemsFromFirestore();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                // Refresh data when tab is reselected
                fetchItemsFromFirestore();
            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                return true;
            } else if (item.getItemId() == R.id.nav_post) {
                startActivity(new Intent(MainActivity.this, PostItemActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        });

        // Initial data load
        fetchItemsFromFirestore();
    }
    
    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, 
                R.array.category_array, 
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    currentCategory = ""; // All categories
                } else {
                    currentCategory = parent.getItemAtPosition(position).toString();
                }
                
                // When category changes, reset and refetch data from Firebase
                if (!currentCategory.isEmpty()) {
                    fetchItemsFromFirestore();
                } else {
                    applyAllFilters();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentCategory = "";
                applyAllFilters();
            }
        });
    }
    
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query.toLowerCase().trim();
                
                // When submitting a search, reset and refetch data
                if (!currentQuery.isEmpty()) {
                    fetchItemsFromFirestore();
                } else {
                    applyAllFilters();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText.toLowerCase().trim();
                applyAllFilters();
                return true;
            }
        });
    }
    
    private void applyAllFilters() {
        // First filter by tab (lost/found/all)
        filterByTabPosition();
        
        // Then apply search query and category filters
        if (!currentQuery.isEmpty() || !currentCategory.isEmpty()) {
            List<Item> filteredList = new ArrayList<>();
            
            for (Item item : allItems) {
                boolean matchesQuery = currentQuery.isEmpty() || 
                        item.getTitle().toLowerCase().contains(currentQuery) ||
                        item.getDescription().toLowerCase().contains(currentQuery) ||
                        item.getLocation().toLowerCase().contains(currentQuery);
                
                boolean matchesCategory = currentCategory.isEmpty() || 
                        item.getCategory().equalsIgnoreCase(currentCategory);
                
                if (matchesQuery && matchesCategory) {
                    filteredList.add(item);
                }
            }
            
            allItems.clear();
            allItems.addAll(filteredList);
            
            // Update pagination for filtered results
            currentPage = 1;
            updatePageInfo();
            updatePaginatedList();
        }
        
        itemAdapter.notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
        tvEmptyView.setVisibility(allItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void fetchItemsFromFirestore() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyView.setVisibility(View.GONE);

        db.collection("All Items")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    if (!queryDocumentSnapshots.isEmpty()) {
                        allItems.clear();
                        
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Item item = document.toObject(Item.class);
                            allItems.add(item);
                        }

                        // Reset to first page when fetching new data
                        currentPage = 1;
                        updatePageInfo();
                        updatePaginatedList();
                        
                        // Apply filtering with all current filters
                        applyAllFilters();
                    } else {
                        allItems.clear();
                        updatePageInfo();
                        updatePaginatedList();
                        applyAllFilters();
                    }

                    tvEmptyView.setVisibility(allItems.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(MainActivity.this, "Error loading items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("FirestoreError", "Error loading items", e);
                });
    }

    private void filterByTabPosition() {
        List<Item> filteredList = new ArrayList<>();

        switch (currentTabPosition) {
            case 0: // All Items
                filteredList.addAll(allItems);
                break;
            case 1: // Lost Items
                for (Item item : allItems) {
                    if ("Lost".equals(item.getItemType())) {
                        filteredList.add(item);
                    }
                }
                break;
            case 2: // Found Items
                for (Item item : allItems) {
                    if ("Found".equals(item.getItemType())) {
                        filteredList.add(item);
                    }
                }
                break;
        }

        allItems.clear();
        allItems.addAll(filteredList);
        currentPage = 1;
        updatePageInfo();
        updatePaginatedList();
    }

    private void updatePageInfo() {
        int maxPages = (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE);
        tvPageInfo.setText(String.format("Page %d/%d", currentPage, maxPages));
        btnPrevious.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < maxPages);
    }

    private void updatePaginatedList() {
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());
        
        itemList.clear();
        itemList.addAll(allItems.subList(startIndex, endIndex));
        itemAdapter.notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset bottom navigation selection
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        
        // Refresh the items list from Firestore when returning to this activity
        fetchItemsFromFirestore();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_contact_admin) {
            // Show dialog to contact admin
            showContactAdminDialog();
            return true;
        } else if (id == R.id.action_logout) {
            // Sign out from both Firebase and Google
            signOut();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void signOut() {
        // Sign out from Firebase
        auth.signOut();
        
        // Sign out from Google and clear credentials
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
                
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Clear WebView cookies which can store Google login state
        android.webkit.CookieManager.getInstance().removeAllCookies(null);
        android.webkit.CookieManager.getInstance().flush();
        
        // Revoke access and sign out from Google
        googleSignInClient.revokeAccess().addOnCompleteListener(revokeTask -> {
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                // After sign out, redirect to login screen (if not already redirected by authListener)
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }
    
    private void showContactAdminDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Contact Admin");
        
        // Inflate a custom layout for the dialog
        View view = getLayoutInflater().inflate(R.layout.dialog_contact_admin, null);
        final EditText etMessage = view.findViewById(R.id.et_message);
        
        builder.setView(view);
        
        builder.setPositiveButton("Send", (dialog, which) -> {
            String message = etMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(message)) {
                // Send message to admin
                sendMessageToAdmin(message);
            } else {
                Toast.makeText(MainActivity.this, "Please enter a message", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        builder.create().show();
    }
    
    private void sendMessageToAdmin(String message) {
        // Get current user information
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Create a message object
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("userId", user.getUid());
            messageData.put("userEmail", user.getEmail());
            messageData.put("message", message);
            messageData.put("timestamp", new Date());
            messageData.put("isRead", false);
            
            // Save to Firestore
            db.collection("admin_messages")
                    .add(messageData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(MainActivity.this, "Message sent to admin", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Failed to send message: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }
}