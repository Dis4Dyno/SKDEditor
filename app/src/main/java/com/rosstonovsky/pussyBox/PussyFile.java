package com.rosstonovsky.pussyBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PussyFile extends File {

	private final String busyboxPath = "." + PussyUser.getAppFilesFolder() + "/bin/busybox ";

	public PussyFile(@NonNull String pathname) {
		super(pathname);
	}

	public PussyFile(@Nullable String parent, @NonNull String child) {
		super(parent, child);
	}

	public PussyFile(@Nullable File parent, @NonNull String child) {
		super(parent, child);
	}

	public PussyFile(@NonNull URI uri) {
		super(uri);
	}

	@Override
	public boolean isFile() {
		List<String> stdout = new PussyShell().busybox("stat -c %F \"" + getAbsolutePath() + "\"");
		if (stdout.size() != 0) {
			return stdout.get(0).contains("file");
		}
		return false;
	}

	@Override
	public boolean exists() {
		List<String> stdout = new ArrayList<>();
		List<String> stderr = new ArrayList<>();
		new PussyShell().cmd(busyboxPath + "ls \"" + getAbsolutePath() + "\"").to(stdout, stderr).exec();
		return stderr.size() == 0;
	}

	@Override
	public boolean isDirectory() {
		List<String> stdout = new PussyShell().busybox("stat -c %F \"" + getAbsolutePath() + "\"");

		if (stdout.size() != 0) {
			return stdout.get(0).contains("directory");
		}
		return false;
	}

	public boolean isLink() {
		List<String> stdout = new PussyShell().busybox("stat -c %F \"" + getAbsolutePath() + "\"");

		if (stdout.size() != 0) {
			return stdout.get(0).contains("symbolic link");
		}
		return false;

	}

	@Override
	public boolean delete() {
		List<String> stdout = new ArrayList<>();
		List<String> stderr = new ArrayList<>();
		new PussyShell().cmd(busyboxPath + "rm -rf \"" + getAbsolutePath() + "\"").to(stdout, stderr).exec();
		return stderr.size() == 0;
	}

	@Nullable
	@Override
	public File[] listFiles() {
		List<PussyFile> allFiles = new ArrayList<>();
		List<String> files = new PussyShell().busybox("ls -1 -p \"" + getAbsolutePath() + "\"");
		List<PussyFile> sortedFolders = new ArrayList<>();
		List<PussyFile> sortedFiles = new ArrayList<>();
		for (int i = 0; i < files.size(); i++) {
			String currName = files.get(i);
			if (currName.endsWith("/")) {
				sortedFolders.add(new PussyFile(getAbsolutePath() + "/" + currName.substring(0, currName.length() - 1)));
			} else {
				sortedFiles.add(new PussyFile(getAbsolutePath() + "/" + currName));
			}
		}
		allFiles.addAll(sortedFolders);
		allFiles.addAll(sortedFiles);
		return allFiles.toArray(new PussyFile[0]);
	}

	/**
	 * Returns permission, user id, group id
	 */
	public int[] getProperties() {
		List<String> stdout = new PussyShell().busybox("stat -c \"%a %u %g\" \"" + getAbsolutePath() + "\"");
		String[] propertiesArr = stdout.get(0).split("\\s");
		int[] properties = new int[3];
		int i = 0;
		for (String prop : propertiesArr) {
			properties[i] = Integer.parseInt(prop);
			i++;
		}
		return properties;
	}

	public void setProperties(int[] properties) {
		PussyShell shell = new PussyShell();
		shell.busybox("chmod -R " + properties[0] + " \"" + getAbsolutePath() + "\"");
		shell.busybox("chown -R " + properties[1] + " \"" + getAbsolutePath() + "\"");
		shell.busybox("chgrp -R " + properties[2] + " \"" + getAbsolutePath() + "\"");
	}

	@Override
	public boolean createNewFile() throws IOException {
		List<String> stdout = new ArrayList<>();
		List<String> stderr = new ArrayList<>();
		new PussyShell().cmd("> \"" + getAbsolutePath() + "\"").to(stdout, stderr).exec();
		if (stderr.size() != 0) {
			StringBuilder sb = new StringBuilder();
			for (String s : stderr) {
				sb  .append(s)
					.append("\n");
			}
			throw new IOException(sb.toString());
		}
		return true;
	}

	@Override
	public boolean mkdir() {
		List<String> stdout = new ArrayList<>();
		List<String> stderr = new ArrayList<>();
		new PussyShell().cmd(busyboxPath + "mkdir \"").to(stdout, stderr).exec();
		return stderr.size() == 0;
	}

	@Override
	public boolean mkdirs() {
		List<String> stdout = new ArrayList<>();
		List<String> stderr = new ArrayList<>();
		new PussyShell().cmd(busyboxPath + "mkdir -p \"").to(stdout, stderr).exec();
		return stderr.size() == 0;
	}

	public void copyTo(String path) throws IOException {
		PussyFile destionation = new PussyFile(path);
		copyTo(destionation);
	}

	public void copyTo(PussyFile destionation) throws IOException {
		if (!destionation.exists()) {
			destionation.mkdirs();
		}
		List<String> stdout = new ArrayList<>();
		List<String> stderr = new ArrayList<>();
		new PussyShell().cmd(busyboxPath + "cp -rf \"" + getAbsolutePath() + "\" " + "\"" + destionation.getAbsolutePath() + "\"").to(stdout, stderr).exec();
		if (stderr.size() != 0) {
			StringBuilder sb = new StringBuilder();
			for (String s : stderr) {
				sb  .append(s)
						.append("\n");
			}
			throw new IOException(sb.toString());
		}
		destionation.setProperties(destionation.getProperties());
		if (stderr.size() != 0) {
			StringBuilder sb = new StringBuilder();
			for (String s : stderr) {
				sb  .append(s)
						.append("\n");
			}
			throw new IOException(sb.toString());
		}
	}

	public File getFile() throws IOException {
		PussyFile cacheFolder = new PussyFile(PussyUser.getAppDataFolder() + "/cache/");
		copyTo(cacheFolder);
		return new File(PussyUser.getAppDataFolder() + "/cache/" + getName());
	}

	/**
	 * In order to save changes from getFile() you need to call this method
	 */
	public void commit() throws IOException {
		PussyFile pussyFile = new PussyFile(PussyUser.getAppDataFolder() + "/cache/" + getName());
		if (!pussyFile.exists()) {
			throw new IOException("File " + getName() + " doesn't exist");
		}
		pussyFile.copyTo(getParent());
	}

	@Override
	public long lastModified() {
		List<String> stdout = new ArrayList<>();
		List<String> stderr = new ArrayList<>();
		new PussyShell().cmd(busyboxPath + "date +%s -r \"" + getAbsolutePath() + "\"").to(stdout, stderr).exec();
		if (stdout.size() != 0) {
			return Long.parseLong(stdout.get(0));
		}
		return 0;
	}

	@Override
	public long length() {
		List<String> stdout = new ArrayList<>();
		List<String> stderr = new ArrayList<>();
		new PussyShell().cmd(busyboxPath + "stat -c %s \"" + getAbsolutePath() + "\"").to(stdout, stderr).exec();
		if (stdout.size() != 0) {
			return Long.parseLong(stdout.get(0));
		}
		return 0;
	}
}