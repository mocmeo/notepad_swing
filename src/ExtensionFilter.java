
import java.io.File;
import javax.swing.filechooser.FileFilter;

public class ExtensionFilter extends FileFilter {

    private String extension;
    private String description;

    public ExtensionFilter(String extension, String description) {
        this.extension = extension;
        this.description = description;
    }
    
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        } else {
            return file.getName().toLowerCase().endsWith(extension);
        }
    }
    
    public String getDescription() {
        return description + String.format(" (*%s)", extension);
    }
}
