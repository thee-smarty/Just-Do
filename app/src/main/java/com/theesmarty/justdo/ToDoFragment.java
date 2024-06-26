package com.theesmarty.justdo;

// TODO: 6/16/24 Add more responsiveness when user performs an action then it has to update the whole fragment 

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ToDoFragment extends Fragment {

    private ArrayList<String> tasks;
    private ArrayAdapter<String> adapter;
    private CollectionReference tasksRef;
    private CollectionReference completedRef;
    private TextView infoView;
    private ListView listView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tasks = new ArrayList<>();
        FirebaseFirestore firestoreFirebase = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            tasksRef = firestoreFirebase.collection("Just Do").document(userId).collection("tasks-todo");
            completedRef = firestoreFirebase.collection("Just Do").document(userId).collection("tasks-completed");
            fetchTasksFromFirestore();
        } else {
            Toast.makeText(getActivity(), "User Error", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_todo, container, false);
        listView = view.findViewById(R.id.list);
        Button addButton = view.findViewById(R.id.add);
        infoView = view.findViewById(R.id.info);

        adapter = new ArrayAdapter<String>(getContext(), R.layout.list_item, R.id.textView, tasks) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(getContext().LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(R.layout.list_item, parent, false);
                }

                CheckBox checkBox = convertView.findViewById(R.id.checkBox);
                TextView textView = convertView.findViewById(R.id.textView);

                String task = getItem(position);
                textView.setText(task);

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        moveTaskToCompleted(task);
                    }
                });

                convertView.setOnLongClickListener(v -> {
                    showDeleteTaskDialog(task);
                    return true;
                });

                return convertView;
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
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("id", taskId);
        taskData.put("task", task);

        tasksRef.document(taskId).set(taskData).addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                tasks.add(task);
                adapter.notifyDataSetChanged();
                updateInfoVisibility();
            } else {
                Toast.makeText(getActivity(), "Error 1ATF", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String generateUniqueId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void moveTaskToCompleted(String task) {
        tasksRef.whereEqualTo("task", task).get().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful() && task1.getResult() != null) {
                for (DocumentSnapshot document : task1.getResult()) {
                    String taskId = document.getId();
                    completedRef.document(taskId).set(Objects.requireNonNull(document.getData())).addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            document.getReference().delete();
                            tasks.remove(task);
                            adapter.notifyDataSetChanged();
                            updateInfoVisibility();
                        } else {
                            Toast.makeText(getActivity(), "Error 1ATC", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(getActivity(), "Error 1RFT", Toast.LENGTH_SHORT).show();
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
        tasksRef.whereEqualTo("task", task).get().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful() && task1.getResult() != null) {
                for (DocumentSnapshot document : task1.getResult()) {
                    document.getReference().delete().addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            tasks.remove(task);
                            adapter.notifyDataSetChanged();
                            updateInfoVisibility();
                        } else {
                            Toast.makeText(getActivity(), "Error 1DTT", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(getActivity(), "Error 1GFT", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTasksFromFirestore() {
        tasksRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    String taskName = document.getString("task");
                    if (taskName != null) {
                        tasks.add(taskName);
                    } else {
                        Toast.makeText(getActivity(), "Error: task name is null", Toast.LENGTH_SHORT).show();
                    }
                }
                adapter.notifyDataSetChanged();
                updateInfoVisibility();
            } else {
                Toast.makeText(getActivity(), "Error 1FTF", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
