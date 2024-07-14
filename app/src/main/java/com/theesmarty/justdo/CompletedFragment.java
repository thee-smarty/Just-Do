package com.theesmarty.justdo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CompletedFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private ArrayAdapter<String> taskAdapter;
    private List<String> taskList;
    private List<String> taskIds;
    ListView list;
    TextView info;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_completed, container, false);

        list = view.findViewById(R.id.list);
        info = view.findViewById(R.id.info);

        taskList = new ArrayList<>();
        taskIds = new ArrayList<>();

        taskAdapter = new ArrayAdapter<String>(getContext(), R.layout.list_item, R.id.task, taskList){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
                }

                String task = getItem(position);

                CheckBox taskCheckbox = convertView.findViewById(R.id.checkBox);
                taskCheckbox.setChecked(true);
                TextView taskText = convertView.findViewById(R.id.task);

                taskText.setText(task);

                taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (!isChecked) {
                        moveTaskToTodo(position);
                    }
                });

                View finalConvertView = convertView;
                convertView.setOnLongClickListener(v -> {
                    showPopup(finalConvertView,position);
                    return true;
                });

                return convertView;
            }
        };
        list.setAdapter(taskAdapter);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if(user!=null){
            loadTasks(user.getUid());
        }

        return view;
    }

    private void showPopup(View view, int position) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenu().add("Delete").setOnMenuItemClickListener(item -> {
            deleteTask(position);
            return true;
        });
        popup.show();
    }

    private void deleteTask(int position) {
        String taskId = taskIds.get(position);
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            firestore.collection("JustDo").document(userId).collection("tasks-completed").document(taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Success", Toast.LENGTH_SHORT).show();
                        loadTasks(userId);
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete note: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void moveTaskToTodo(int position) {
        String taskId = taskIds.get(position);
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference taskRef = firestore.collection("JustDo").document(userId).collection("tasks-completed").document(taskId);

            taskRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> taskData = documentSnapshot.getData();
                    if (taskData != null) {
                        firestore.collection("JustDo").document(userId).collection("tasks-todo").document(taskId)
                                .set(taskData)
                                .addOnSuccessListener(aVoid -> {
                                    taskRef.delete()
                                            .addOnSuccessListener(aVoid1 -> {
                                                Toast.makeText(getContext(), "Task moved to todo", Toast.LENGTH_SHORT).show();
                                                loadTasks(userId);
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to move task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            });
        }
    }

    private void loadTasks(String userId) {
        firestore.collection("JustDo").document(userId).collection("tasks-completed")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        taskList.clear();
                        taskIds.clear();
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String taskName = document.getString("task");
                            String taskId = document.getId();
                            taskList.add(taskName);
                            taskIds.add(taskId);
                        }
                        taskAdapter.notifyDataSetChanged();

                        if (taskList.size() > 0) {
                            list.setVisibility(ListView.VISIBLE);
                            info.setVisibility(TextView.INVISIBLE);
                        } else {
                            info.setVisibility(TextView.VISIBLE);
                            list.setVisibility(ListView.INVISIBLE);
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to load Tasks."+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
