package com.chichar.skdeditor.fragments.explorer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.chichar.skdeditor.Const;
import com.chichar.skdeditor.R;
import com.chichar.skdeditor.activities.EditorActivity;
import com.chichar.skdeditor.gamefiles.GameFileResolver;
import com.chichar.skdeditor.gamefiles.IGameFile;
import com.rosstonovsky.pussyBox.PussyFile;
import com.rosstonovsky.pussyBox.PussyUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ExplorerFragment extends Fragment {
	private static final List<String> gameFiles = new ArrayList<String>() {
		{
			add("files");
			add("shared_prefs");
		}
	};

	@SuppressLint("StaticFieldLeak")
	private static Context explorerContext;
	@SuppressLint("StaticFieldLeak")
	private static ListView explorer;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	private View view;

	@SuppressLint("SdCardPath")
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_explorer, container, false);
		explorer = view.findViewById(R.id.explorer);
		explorerContext = requireContext();

		PussyFile skDir = new PussyFile(PussyUser.getDataFolder() + "/" + Const.pkg);

		if (skDir.exists()) {
			openInExplorer(skDir.getAbsolutePath());
			return view;
		}
		TextView error = view.findViewById(R.id.error);
		error.setText("Soul Knight isn't installed");
		error.setVisibility(View.VISIBLE);
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	public void openInExplorer(String path) {
		SharedPreferences prefs = explorerContext.getSharedPreferences("com.chichar.skdeditor", Context.MODE_PRIVATE);
		boolean clearGarbage = prefs.getBoolean("clearGarbage", true);
		ArrayList<ExplorerFile> explorerFiles = new ArrayList<>();
		PussyFile currFolder = new PussyFile(path);

		if (!currFolder.exists()) {
			Toast.makeText(explorerContext, "Doesn't exist", Toast.LENGTH_SHORT).show();
			return;
		}

		if (currFolder.isFile()) {
			if (currFolder.length() < 8_000_000) {
				Intent intent = new Intent(explorerContext, EditorActivity.class);
				intent.putExtra("path", currFolder.getAbsolutePath());
				explorerContext.startActivity(intent);
				return;
			}
			Toast.makeText(explorerContext, "File is too long", Toast.LENGTH_SHORT).show();
			return;
		}

		if (!(currFolder.getName().equals(Const.pkg))) {
			explorerFiles.add(new ExplorerFile("...", currFolder.getParent(), false, true, false));
		}

		if (gameFiles.size() == 2)
			for (IGameFile gameFile : GameFileResolver.getGameFiles())
				gameFiles.add(new PussyFile(gameFile.getPath()).getName());
		List<PussyFile> files = Arrays.asList(Objects.requireNonNull(currFolder.listFiles()));
		List<ExplorerFile> sortedFolders = new ArrayList<>();
		List<ExplorerFile> sortedFiles = new ArrayList<>();
		for (int i = 0; i < files.size(); i++) {
			PussyFile pussyFile = files.get(i);
			if (!clearGarbage) {
				sortedFolders.add(new ExplorerFile(pussyFile.getName(), pussyFile.getPath(), pussyFile.isFile(), pussyFile.isDirectory(), pussyFile.isLink()));
				continue;
			}
			if (gameFiles.contains(pussyFile.getName())) {
				if (pussyFile.isFile()) {
					sortedFiles.add(new ExplorerFile(pussyFile.getName(), pussyFile.getPath(), true, false, false));
					continue;
				} else if (pussyFile.isDirectory()) {
					sortedFolders.add(new ExplorerFile(pussyFile.getName(), pussyFile.getPath(), false, true, false));
					continue;
				}
				sortedFiles.add(new ExplorerFile(pussyFile.getName(), pussyFile.getPath(), false, false, true));
			}
		}
		explorerFiles.addAll(sortedFolders);
		explorerFiles.addAll(sortedFiles);
		ExplorerAdapter explorerAdapter = new ExplorerAdapter(explorerContext, explorerFiles, this::openInExplorer);
		TextView textView = view.findViewById(R.id.path);
		textView.setText(currFolder.getName());
		assert explorer != null : "Explorer is null";
		explorer.setAdapter(explorerAdapter);
	}
}
