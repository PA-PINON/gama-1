/*********************************************************************************************
 * 
 * 
 * 'PickWorkspaceDialog.java', in plugin 'msi.gama.application', is part of the source code of the
 * GAMA modeling and simulation platform.
 * (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 * 
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 * 
 * 
 **********************************************************************************************/
package msi.gama.gui.swt.dialogs;

// import java.awt.GridLayout;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.prefs.*;
import msi.gama.gui.swt.WorkspaceModelsManager;
import org.eclipse.jface.dialogs.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * Dialog that lets/forces a user to enter/select a workspace that will be used when saving all
 * configuration files and settings. This dialog is shown at startup of the GUI just after the
 * splash screen has shown. Inspired by http://hexapixel.com/2009/01/12/rcp-workspaces
 */
public class PickWorkspaceDialog extends TitleAreaDialog {

	/*
	 * The name of the file that tells us that the workspace directory belongs to our application
	 */
	private static final String WS_IDENTIFIER = ".gama_application_workspace";
	// private static final String VERSION_IDENTIFIER = ".gama_version_" +
	// Platform.getProduct().getDefiningBundle().getVersion().toString();
	private static final String MODEL_IDENTIFIER = WorkspaceModelsManager.getCurrentGamaStampString();
	private static final String keyWorkspaceRootDir = "wsRootDir";
	private static final String keyRememberWorkspace = "wsRemember";
	private static final String keyLastUsedWorkspaces = "wsLastUsedWorkspaces";

	/*
	 * This are our preferences we will be using as the IPreferenceStore is not available yet
	 */
	// FIX: Removed the static reference in case it was causing trouble. Issue 240.

	// static Preferences preferences = Preferences.userRoot().node("gama");
	/* Various dialog messages */
	private static final String strMsg =
		"Your workspace is where settings and files of your Gama models will be stored.";
	private static final String strInfo = "Please select a directory that will be the workspace root";
	private static final String strError = "You must set a directory";

	/* Our controls */
	protected Combo workspacePathCombo;
	protected List<String> lastUsedWorkspaces;
	protected Button rememberWorkspaceButton;

	/* Used as separator when we save the last used workspace locations */
	private static final String splitChar = "#";
	/* Max number of entries in the history box */
	private static final int maxHistory = 20;

	/* Whatever the user picks ends up on this variable */
	private String selectedWorkspaceRootLocation;
	private boolean cloning = false;

	/**
	 * Creates a new workspace dialog with a specific image as title-area image.
	 * 
	 * @param switchWorkspace true if we're using this dialog as a switch workspace dialog
	 * @param wizardImage Image to show
	 */
	public PickWorkspaceDialog() {
		super(Display.getDefault().getActiveShell());
		// setTitleImage(IGamaIcons.GAMA_ICON.image());
	}

	@Override
	protected void configureShell(final Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("GAMA Models Workspace");
	}

	public static Preferences getNode() {
		try {
			if ( Preferences.userRoot().nodeExists("gama") ) { return Preferences.userRoot().node("gama"); }
		} catch (BackingStoreException e1) {
			e1.printStackTrace();
		}
		Preferences p = Preferences.userRoot().node("gama");
		try {
			p.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		return p;
	}

	/** Returns whether the user selected "remember workspace" in the preferences */
	public static boolean isRememberWorkspace() {
		return getNode().getBoolean(keyRememberWorkspace, false);
	}

	/**
	 * Returns the last set workspace directory from the preferences
	 * 
	 * @return null if none
	 */
	public static String getLastSetWorkspaceDirectory() {
		return getNode().get(keyWorkspaceRootDir, null);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		setTitle("Choose a Workspace to store your models, settings, etc.");
		setMessage(strMsg);

		try {
			Composite inner = new Composite(parent, SWT.NONE);
			GridLayout l = new GridLayout(5, false);
			// double[][] layout =
			// new double[][] {
			// { 5, LatticeConstants.PREFERRED, 5, 250, 5, LatticeConstants.PREFERRED, 5 },
			// { 5, LatticeConstants.PREFERRED, 5, LatticeConstants.PREFERRED, 40 } };
			inner.setLayout(l);
			inner.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_END |
				GridData.GRAB_HORIZONTAL));

			/* Label on the left */
			CLabel label = new CLabel(inner, SWT.NONE);
			label.setText("GAMA Models Workspace");
			label.setLayoutData(new GridData());

			/* Combo in the middle */
			workspacePathCombo = new Combo(inner, SWT.BORDER);
			workspacePathCombo.setLayoutData(new GridData());
			String wsRoot = getNode().get(keyWorkspaceRootDir, "");
			if ( wsRoot == null || wsRoot.length() == 0 ) {
				wsRoot = getWorkspacePathSuggestion();
			}
			workspacePathCombo.setText(wsRoot);

			/* Checkbox below */
			rememberWorkspaceButton = new Button(inner, SWT.CHECK);
			rememberWorkspaceButton.setText("Remember workspace");
			rememberWorkspaceButton.setLayoutData(new GridData());
			rememberWorkspaceButton.setSelection(getNode().getBoolean(keyRememberWorkspace, false));

			String lastUsed = getNode().get(keyLastUsedWorkspaces, "");
			lastUsedWorkspaces = new ArrayList<String>();
			if ( lastUsed != null ) {
				String[] all = lastUsed.split(splitChar);
				for ( String str : all ) {
					lastUsedWorkspaces.add(str);
				}
			}
			for ( String last : lastUsedWorkspaces ) {
				workspacePathCombo.add(last);
			}

			/* Browse button on the right */
			Button browse = new Button(inner, SWT.PUSH);
			browse.setText("Browse...");
			browse.setLayoutData(new GridData());
			browse.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(final Event event) {
					DirectoryDialog dd = new DirectoryDialog(getParentShell());
					dd.setText("Select Workspace Root");
					dd.setMessage(strInfo);
					dd.setFilterPath(workspacePathCombo.getText());
					String pick = dd.open();
					if ( pick == null ) {
						if ( workspacePathCombo.getText().length() == 0 ) {
							setMessage(strError, IMessageProvider.ERROR);
						}
					} else {
						setMessage(strMsg);
						workspacePathCombo.setText(pick);
					}
				}
			});
			return inner;
		} catch (RuntimeException err) {
			err.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns whatever path the user selected in the dialog.
	 * 
	 * @return Path
	 */
	public String getSelectedWorkspaceLocation() {
		return getSelectedWorkspaceRootLocation();
	}

	/* Suggests a default path based on the user.home/GAMA directory location */
	private String getWorkspacePathSuggestion() {
		StringBuffer buf = new StringBuffer();

		String uHome = System.getProperty("user.home");
		if ( uHome == null ) {
			uHome = "c:";
		}

		buf.append(uHome).append(File.separator).append("gama_workspace");

		return buf.toString();
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {

		/* Clone workspace needs a lot of checks */
		Button clone = createButton(parent, IDialogConstants.IGNORE_ID, "Clone current workspace", false);
		clone.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(final Event arg0) {
				cloneCurrentWorkspace();
				// try {
				// String txt = workspacePathCombo.getText();
				// File workspaceDirectory = new File(txt);
				// if ( !workspaceDirectory.exists() ) {
				// MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
				// "The path entered does not exist. Please enter a valid path.");
				// return;
				// }
				//
				// if ( !workspaceDirectory.canRead() ) {
				// MessageDialog
				// .openError(Display.getDefault().getActiveShell(), "Error",
				// "The currently entered workspace path is not readable. Please check file system permissions.");
				// return;
				// }
				//
				// // check for workspace file (empty indicator that it's a workspace)
				// File wsFile = new File(txt + File.separator + WS_IDENTIFIER);
				// if ( !wsFile.exists() ) {
				// boolean b =
				// MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Confirm cloning",
				// "A new workspace will be created at location " + txt +
				// " and the current workspace contents will be copied to it. Confirm ?");
				// if ( !b ) { return; }
				// if ( !checkAndCreateWorkspaceRoot(txt) ) { return; }
				// } else {
				// boolean b =
				// MessageDialog
				// .openConfirm(
				// Display.getDefault().getActiveShell(),
				// "Existing workspace",
				// "The path entered is a path to an existing workspace. All its contents will be erased and replaced by the current workspace contents. Proceed anyway ?");
				// if ( !b ) { return; }
				// }
				//
				// DirectoryDialog dd = new DirectoryDialog(Display.getDefault().getActiveShell());
				// dd.setFilterPath(txt);
				// String directory = dd.open();
				// if ( directory == null ) { return; }
				//
				// File targetDirectory = new File(directory);
				// if ( targetDirectory.getAbsolutePath().equals(workspaceDirectory.getAbsolutePath()) ) {
				// MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
				// "Source and target workspaces are the same");
				// return;
				// }
				//
				// // recursive check, if new directory is a subdirectory of
				// // our workspace, that's a big no-no or we'll
				// // create directories forever
				// if ( isTargetSubdirOfDir(workspaceDirectory, targetDirectory) ) {
				// MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
				// "The path entered is a subdirectory of the current workspace");
				// return;
				// }
				//
				// try {
				// copyFiles(workspaceDirectory, targetDirectory);
				// } catch (Exception err) {
				// MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
				// "There was an error cloning the workspace: " + err.getMessage());
				// return;
				// }
				//
				// boolean setActive =
				// MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Workspace Cloned",
				// "Would you like to set the newly cloned workspace to be the active one?");
				// if ( setActive ) {
				// workspacePathCombo.setText(directory);
				// }
				// } catch (Exception err) {
				// MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
				// "There was an internal error, please check the logs");
				// err.printStackTrace();
				// }
			}
		});
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/* Checks whether a target directory is a subdirectory of ourselves */
	private boolean isTargetSubdirOfDir(final File source, final File target) {
		List<File> subdirs = new ArrayList<File>();
		getAllSubdirectoriesOf(source, subdirs);
		return subdirs.contains(target);
	}

	/* Helper for above */
	private void getAllSubdirectoriesOf(final File target, final List<File> buffer) {
		File[] files = target.listFiles();
		if ( files == null || files.length == 0 ) { return; }

		for ( File f : files ) {
			if ( f.isDirectory() ) {
				buffer.add(f);
				getAllSubdirectoriesOf(f, buffer);
			}
		}
	}

	/**
	 * This function will copy files or directories from one location to another. note that the
	 * source and the destination must be mutually exclusive. This function can not be used to copy
	 * a directory to a sub directory of itself. The function will also have problems if the
	 * destination files already exist.
	 * 
	 * @param src -- A File object that represents the source for the copy
	 * @param dest -- A File object that represents the destination for the copy.
	 * @throws IOException if unable to copy.
	 */
	public static void copyFiles(final File src, final File dest) throws IOException {
		/* Check to ensure that the source is valid... */
		if ( !src.exists() ) {
			throw new IOException("Can not find source: " + src.getAbsolutePath());
		} else if ( !src.canRead() ) { // check to ensure we have rights to the
										// source...
			throw new IOException("Cannot read: " + src.getAbsolutePath() + ". Check file permissions.");
		}
		/* Is this a directory copy? */
		if ( src.isDirectory() ) {
			/* Does the destination already exist? */
			if ( !dest.exists() ) {
				/* If not we need to make it exist if possible */
				if ( !dest.mkdirs() ) { throw new IOException("Could not create direcotry: " + dest.getAbsolutePath()); }
			}
			/* Get a listing of files... */
			String list[] = src.list();
			/* Copy all the files in the list. */
			for ( int i = 0; i < list.length; i++ ) {
				File dest1 = new File(dest, list[i]);
				File src1 = new File(src, list[i]);
				copyFiles(src1, dest1);
			}
		} else {
			/* This was not a directory, so lets just copy the file */
			FileInputStream fin = null;
			FileOutputStream fout = null;
			byte[] buffer = new byte[4096];
			int bytesRead;
			try {
				/* Open the files for input and output */
				fin = new FileInputStream(src);
				fout = new FileOutputStream(dest);
				/* While bytesRead indicates a successful read, lets write... */
				while ((bytesRead = fin.read(buffer)) >= 0) {
					fout.write(buffer, 0, bytesRead);
				}
			} catch (IOException e) {
				IOException wrapper =
					new IOException("Unable to copy file: " + src.getAbsolutePath() + "to" + dest.getAbsolutePath());
				wrapper.initCause(e);
				wrapper.setStackTrace(e.getStackTrace());
				throw wrapper;
				/* Ensure that the files are closed (if they were open). */
			} finally {
				if ( fin != null ) {
					fin.close();
				}
				if ( fout != null ) {
					fout.close();
				}
			}
		}
	}

	protected void cloneCurrentWorkspace() {
		// Some checks first
		String newLocation = workspacePathCombo.getText();
		String currentLocation = getNode().get(keyWorkspaceRootDir, "");
		if ( currentLocation.isEmpty() ) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
				"No current workspace exists. Can only clone from an existing workspace");
			return;
		}
		File workspaceDirectory = new File(currentLocation);
		File targetDirectory = new File(newLocation);
		if ( targetDirectory.getAbsolutePath().equals(workspaceDirectory.getAbsolutePath()) ) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
				"Please enter a different location for the new workspace. A workspace cannot be cloned into itself.");
			return;
		}
		// recursive check, if new directory is a subdirectory of
		// our workspace, that's a big no-no or we'll
		// create directories forever
		if ( isTargetSubdirOfDir(workspaceDirectory, targetDirectory) ) {
			MessageDialog
				.openError(
					Display.getDefault().getActiveShell(),
					"Error",
					"The path entered is a subdirectory of the current workspace. A workspace cannot be cloned in one of its sub-directories");
			return;
		}
		// If the checks are ok, we set "cloning" to true and do as if ok was pressed.
		cloning = true;
		try {
			okPressed();
		} finally {
			cloning = false;
		}
	}

	@Override
	protected void okPressed() {
		String str = workspacePathCombo.getText();
		// GuiUtils.debug("Directory to create " + str);
		if ( str.length() == 0 ) {
			setMessage(strError, IMessageProvider.ERROR);
			return;
		}

		String ret = checkWorkspaceDirectory(getParentShell(), str, true, true, cloning);
		if ( ret != null ) {
			setMessage(ret, IMessageProvider.ERROR);
			return;
		}
		// GuiUtils.debug("Directory to create (after check " + str);
		/* Save it so we can show it in combo later */
		lastUsedWorkspaces.remove(str);

		if ( !lastUsedWorkspaces.contains(str) ) {
			lastUsedWorkspaces.add(0, str);
		}

		/* Deal with the max history */
		if ( lastUsedWorkspaces.size() > maxHistory ) {
			List<String> remove = new ArrayList<String>();
			for ( int i = maxHistory; i < lastUsedWorkspaces.size(); i++ ) {
				remove.add(lastUsedWorkspaces.get(i));
			}

			lastUsedWorkspaces.removeAll(remove);
		}

		/* Create a string concatenation of all our last used workspaces */
		StringBuffer buf = new StringBuffer();
		for ( int i = 0; i < lastUsedWorkspaces.size(); i++ ) {
			buf.append(lastUsedWorkspaces.get(i));
			if ( i != lastUsedWorkspaces.size() - 1 ) {
				buf.append(splitChar);
			}
		}

		/* Save them onto our preferences */
		getNode().putBoolean(keyRememberWorkspace, rememberWorkspaceButton.getSelection());
		getNode().put(keyLastUsedWorkspaces, buf.toString());
		try {
			getNode().flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		/* Now create it */
		boolean ok = checkAndCreateWorkspaceRoot(str);
		if ( !ok ) {
			// GuiUtils.debug("Problem creating " + str);
			setMessage("No workspace could be created at location " + str + ", please check the error log");
			return;
		}

		/* Here we set the location so that we can later fetch it again */
		setSelectedWorkspaceRootLocation(str);

		/* And on our preferences as well */
		// GuiUtils.debug("Writing " + str + " in the preferences");
		if ( cloning ) {
			String previousLocation = getNode().get(keyWorkspaceRootDir, "");
			File workspaceDirectory = new File(previousLocation);
			File targetDirectory = new File(str);
			try {
				copyFiles(workspaceDirectory, targetDirectory);
			} catch (Exception err) {
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"There was an error cloning the workspace: " + err.getMessage());
				return;
			}
		}
		getNode().put(keyWorkspaceRootDir, str);
		try {
			getNode().flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		super.okPressed();
	}

	/**
	 * Ensures a workspace directory is OK in regards of reading/writing, etc. This method will get
	 * called externally as well.
	 * 
	 * @param parentShell Shell parent shell
	 * @param workspaceLocation Directory the user wants to use
	 * @param askCreate Whether to ask if to create the workspace or not in this location if it does
	 *            not exist already
	 * @param fromDialog Whether this method was called from our dialog or from somewhere else just
	 *            to check a location
	 * @return null if everything is ok, or an error message if not
	 */
	public static String checkWorkspaceDirectory(final Shell parentShell, final String workspaceLocation,
		final boolean askCreate, final boolean fromDialog, final boolean cloning) {
		File f = new File(workspaceLocation);
		if ( !f.exists() ) {
			if ( askCreate ) {
				boolean create =
					MessageDialog.openConfirm(parentShell, "New Directory", workspaceLocation +
						" does not exist. Would you like to create a new workspace here" +
						(cloning ? ", copy the contents of the current workspace into it, " : "") +
						" and restart the application ?");
				if ( create ) {
					try {
						f.mkdirs();
						File wsDot = new File(workspaceLocation + File.separator + WS_IDENTIFIER);
						wsDot.createNewFile();
						File dotFile = new File(workspaceLocation + File.separator + MODEL_IDENTIFIER);
						dotFile.createNewFile();
					} catch (RuntimeException err) {
						// GuiUtils
						// .debug("Error creating directories, please check folder permissions");
						err.printStackTrace();
						return "Error creating directories, please check folder permissions";
					} catch (IOException er) {
						er.printStackTrace();
						return "Error creating directories, please check folder permissions";
					}
				}

				if ( !f.exists() ) {
					return "The selected directory does not exist";
				} else {
					return null;
				}
			}
		}

		if ( !f.canRead() ) {
			// GuiUtils.debug("The selected directory is not readable");
			return "The selected directory is not readable";
		}

		if ( !f.isDirectory() ) {
			// GuiUtils.debug("The selected path is not a directory");
			return "The selected path is not a directory";
		}

		File wsTest = new File(workspaceLocation + File.separator + WS_IDENTIFIER);
		if ( fromDialog ) {
			if ( !wsTest.exists() ) {
				boolean create =
					MessageDialog
						.openConfirm(parentShell, "New Workspace", "The directory '" + wsTest.getAbsolutePath() +
							"' exists but is not identified as a GAMA workspace. \n\nWould you like to use it anyway ?");
				if ( create ) {
					try {
						f.mkdirs();
						File wsDot = new File(workspaceLocation + File.separator + WS_IDENTIFIER);
						wsDot.createNewFile();
					} catch (Exception err) {
						// GuiUtils
						// .debug("Error creating directories, please check folder permissions");
						return "Error creating directories, please check folder permissions";
					}
				} else {
					// GuiUtils.debug("Please select a directory for your workspace");
					return "Please select a directory for your workspace";
				}

				if ( !wsTest.exists() ) {
					// GuiUtils.debug("The selected directory does not exist");
					return "The selected directory does not exist";
				}

				return null;
			}
		} else {
			if ( !wsTest.exists() ) { return "The selected directory is not a workspace directory"; }
		}
		File dotFile = new File(workspaceLocation + File.separator + MODEL_IDENTIFIER);
		if ( !dotFile.exists() ) {
			if ( fromDialog ) {
				boolean create =
					MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
						"Outdated version of the models library",
						"The workspace contains an old version of the models library. Do you want to proceed anyway ?");
				if ( create ) {
					try {
						dotFile.createNewFile();
					} catch (IOException e) {
						return "Error updating the models library";
					}
					return null;
				}
			}

			return "models";
		} else if ( cloning ) {
			boolean b =
				MessageDialog
					.openConfirm(
						Display.getDefault().getActiveShell(),
						"Existing workspace",
						"The path entered is a path to an existing workspace. All its contents will be erased and replaced by the current workspace contents. Proceed anyway ?");
			if ( !b ) { return "no clone"; }
		}
		return null;
	}

	/**
	 * Checks to see if a workspace exists at a given directory string, and if not, creates it. Also
	 * puts our identifying file inside that workspace.
	 * 
	 * @param wsRoot Workspace root directory as string
	 * @return true if all checks and creations succeeded, false if there was a problem
	 */
	public static boolean checkAndCreateWorkspaceRoot(final String wsRoot) {
		try {
			File fRoot = new File(wsRoot);
			if ( !fRoot.exists() ) {
				// GuiUtils.debug("Folder " + wsRoot + " does not exist");
				return false;
			}

			File dotFile = new File(wsRoot + File.separator + WS_IDENTIFIER);
			if ( !dotFile.exists() ) {
				boolean created = dotFile.createNewFile();
				if ( !created ) { return false; }
				dotFile = new File(wsRoot + File.separator + MODEL_IDENTIFIER);
				dotFile.createNewFile();
			}

			return true;
		} catch (Exception err) {
			err.printStackTrace();
			return false;
		}
	}

	private String getSelectedWorkspaceRootLocation() {
		return selectedWorkspaceRootLocation;
	}

	private void setSelectedWorkspaceRootLocation(final String selectedWorkspaceRootLocation) {
		this.selectedWorkspaceRootLocation = selectedWorkspaceRootLocation;
	}

}
