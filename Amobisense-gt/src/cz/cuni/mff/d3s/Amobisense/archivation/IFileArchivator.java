package cz.cuni.mff.d3s.Amobisense.archivation;

import java.io.InputStream;

public interface IFileArchivator {
	boolean addFileToArchive(InputStream fIn, String fileNameInArchive);
}
