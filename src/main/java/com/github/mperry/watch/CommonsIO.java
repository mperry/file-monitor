package com.github.mperry.watch;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by MarkPerry on 18/08/2014.
 */

/**
 * Copied from FileUtils commons-io 2.4
 * Conflicts with built in commons-io 1.4 for Gradle
 */
public class CommonsIO {

	public static void writeStringToFile(final File file, final String data, final boolean append) throws IOException {

		OutputStream out = null;
		try {
			out = openOutputStream(file, append);
			IOUtils.write(data, out);
			out.close(); // don't swallow close Exception if copy completes normally
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	public static FileOutputStream openOutputStream(final File file, final boolean append) throws IOException {
		if (file.exists()) {
			if (file.isDirectory()) {
				throw new IOException("File '" + file + "' exists but is a directory");
			}
			if (file.canWrite() == false) {
				throw new IOException("File '" + file + "' cannot be written to");
			}
		} else {
			final File parent = file.getParentFile();
			if (parent != null) {
				if (!parent.mkdirs() && !parent.isDirectory()) {
					throw new IOException("Directory '" + parent + "' could not be created");
				}
			}
		}
		return new FileOutputStream(file, append);
	}


}
