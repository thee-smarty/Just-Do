package com.theesmarty.justdo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class CompletedFragment extends Fragment {

    private ArrayList<String> completedTasks;
    private ArrayAdapter<String> adapter;
    private FirebaseFirestore firestoreFirebase;
    private CollectionReference completedRef;
    private CollectionReference tasksRef;
    private String userId;
    private TextView infoView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        completedTasks = new ArrayList<>();
        firestoreFirebase = FirebaseFirestore.getInstance();

        // Get current user ID
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            completedRef = firestoreFirebase.collection("Just Do").document(userId).collection("completed");
            tasksRef = firestoreFirebase.collection("Just Do").document(userId).collection("tasks");
            fetchCompletedTasksFromFirestore();
        } else {
            // Handle user not logged in
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_completed, container, false);

        ListView listView = view.findViewById(R.id.completed_list);
        infoView = view.findViewById(R.id.completed_info);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_checked, completedTasks);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            CheckBox checkBox = (CheckBox) view1;
            if (!checkBox.isChecked()) {
                String task = completedTasks.get(position);
                moveTaskToPending(task);
            }
        });

        updateInfoVisibility();

        return view;
    }

    private void updateInfoVisibility() {
        if (completedTasks.isEmpty()) {
            infoView.setVisibility(View.VISIBLE);
        } else {
            infoView.setVisibility(View.GONE);
        }
    }

    private void moveTaskToPending(String task) {
        completedRef.whereEqualTo("name", task).get().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful() && task1.getResult() != null) {
                for (DocumentSnapshot document : task1.getResult()) {
                    TaskModel taskModel = document.toObject(TaskModel.class);
                    tasksRef.add(taskModel).addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            document.getReference().delete();
                            completedTasks.remove(taskModel.getName());
                            adapter.notifyDataSetChanged();
                            updateInfoVisibility();
                        } else {
                            // Handle error
                        }
                    });
                }
            } else {
                // Handle error
            }
        });
    }

    private void fetchCompletedTasksFromFirestore() {
        completedRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    TaskModel taskModel = document.toObject(TaskModel.class);
                    completedTasks.add(taskModel.getName());
                }
                adapter.notifyDataSetChanged();
                updateInfoVisibility();
            } else {
                // Handle error
            }
        });
    }

    private static class TaskModel {
        private String name;

        public TaskModel() {
            // Default constructor required for calls to DataSnapshot.getValue(TaskModel.class)
        }

        public TaskModel(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}