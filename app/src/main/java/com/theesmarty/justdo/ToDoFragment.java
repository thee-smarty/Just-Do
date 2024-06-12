package com.theesmarty.justdo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ToDoFragment extends Fragment {

    private ArrayList<String> tasks;
    private ArrayAdapter<String> adapter;
    private FirebaseFirestore firestoreFirebase;
    private CollectionReference tasksRef;
    private CollectionReference completedRef;
    private String userId;
    private TextView infoView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tasks = new ArrayList<>();
        firestoreFirebase = FirebaseFirestore.getInstance();

        // Get current user ID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            tasksRef = firestoreFirebase.collection("Just Do").document(userId).collection("tasks-todo");
            completedRef = firestoreFirebase.collection("Just Do").document(userId).collection("tasks-completed");
            fetchTasksFromFirestore();
        } else {
            // Handle user not logged in
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_to_do, container, false);

        ListView listView = view.findViewById(R.id.list);
        Button addButton = view.findViewById(R.id.add);
        infoView = view.findViewById(R.id.info);

        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, tasks) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View itemView = super.getView(position, convertView, parent);

                CheckBox checkBox = itemView.findViewById(R.id.checkBox);
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        String task = tasks.get(position);
                        moveTaskToCompleted(task);
                    }
                });

                return itemView;
            }
        };

        listView.setAdapter(adapter);

        addButton.setOnClickListener(v -> showAddTaskDialog());

        updateInfoVisibility();

        return view;
    }

    private void updateInfoVisibility() {
        if (tasks.isEmpty()) {
            infoView.setVisibility(View.VISIBLE);
        } else {
            infoView.setVisibility(View.GONE);
        }
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Task");

        final EditText input = new EditText(getContext());
        input.setHint("Enter task here");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String task = input.getText().toString().trim();
            if (!task.isEmpty()) {
                addTaskToFirestore(task);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addTaskToFirestore(String task) {
        String taskId = generateUniqueId();
        tasksRef.document(taskId).set(new Task(taskId, task)).addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                tasks.add(task);
                adapter.notifyDataSetChanged();
                updateInfoVisibility();
            } else {
                // Handle failure
            }
        });
    }

    private String generateUniqueId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void moveTaskToCompleted(String task) {
        tasksRef.whereEqualTo("name", task).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (DocumentSnapshot document : task.getResult()) {
                        String taskId = document.getId();
                        completedRef.document(taskId).set(document.getData()).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                if (task.isSuccessful()) {
                                    document.getReference().delete();
                                    tasks.remove(task);
                                    adapter.notifyDataSetChanged();
                                    updateInfoVisibility();
                                } else {
                                    // Handle error
                                }
                            }
                        });
                    }
                } else {
                    // Handle error
                }
            }
        });
    }

    private void fetchTasksFromFirestore() {
        tasksRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        tasks.add(document.getString("name"));
                    }
                    adapter.notifyDataSetChanged();
                    updateInfoVisibility();
                } else {
                    // Handle error
                }
            }
        });
    }
}
