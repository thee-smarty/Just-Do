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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            completedRef = firestoreFirebase.collection("Just Do").document(userId).collection("tasks-completed");
            tasksRef = firestoreFirebase.collection("Just Do").document(userId).collection("tasks-todo");
            fetchCompletedTasksFromFirestore();
        } else {
            Toast.makeText(getActivity(), "User Error", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_completed, container, false);

        ListView listView = view.findViewById(R.id.list);
        infoView = view.findViewById(R.id.info);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_multiple_choice, completedTasks);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            CheckBox checkBox = (CheckBox) view1.findViewById(android.R.id.checkbox);
            if (checkBox != null && !checkBox.isChecked()) {
                String task = completedTasks.get(position);
                moveTaskToPending(task);
            }
        });

        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
            String task = completedTasks.get(position);
            showDeleteTaskDialog(task);
            return true;
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
        completedRef.whereEqualTo("task", task).get().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful() && task1.getResult() != null) {
                for (DocumentSnapshot document : task1.getResult()) {
                    tasksRef.document(document.getId()).set(document.getData()).addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            document.getReference().delete();
                            completedTasks.remove(task);
                            adapter.notifyDataSetChanged();
                            updateInfoVisibility();
                        } else {
                            Toast.makeText(getActivity(), "Error 1ATT", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(getActivity(), "Error 1RTC", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteTaskDialog(String task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Delete Task");
        builder.setMessage("Are you sure you want to delete this task?");
        builder.setPositiveButton("Yes", (dialog, which) -> deleteTaskFromFirestore(task));
        builder.setNegativeButton("No", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void deleteTaskFromFirestore(String task) {
        completedRef.whereEqualTo("task", task).get().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful() && task1.getResult() != null) {
                for (DocumentSnapshot document : task1.getResult()) {
                    document.getReference().delete().addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            completedTasks.remove(task);
                            adapter.notifyDataSetChanged();
                            updateInfoVisibility();
                        } else {
                            Toast.makeText(getActivity(), "Error 1DTC", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(getActivity(), "Error 1GFC", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchCompletedTasksFromFirestore() {
        completedRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    completedTasks.add(document.getString("task"));
                }
                adapter.notifyDataSetChanged();
                updateInfoVisibility();
            } else {
                Toast.makeText(getActivity(), "Error 1FTF", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
