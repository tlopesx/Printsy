package gallery.dto;

import java.io.Serializable;
import jakarta.persistence.*;

@Embeddable
public class UserImageId implements Serializable {
    private String userId;
    private String imageId;

    public UserImageId() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
}
