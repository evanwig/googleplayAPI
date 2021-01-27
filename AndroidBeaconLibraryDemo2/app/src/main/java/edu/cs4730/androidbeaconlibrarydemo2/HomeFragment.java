package edu.cs4730.androidbeaconlibrarydemo2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import edu.cs4730.androidbeaconlibrarydemo2.R;

public class HomeFragment extends Fragment {

    TextView logger;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);
       logger = root.findViewById(R.id.logger);
        return root;
    }

    public void logthis(String item) {
        if (logger != null)
        logger.append(item + "\n");
    }

}