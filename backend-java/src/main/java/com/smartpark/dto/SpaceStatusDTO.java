package com.smartpark.dto;

public class SpaceStatusDTO {
    private String type;
    private Long spaceId;
    private String spaceCode;
    private String status;
    private String color;
    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private String name;

    public SpaceStatusDTO() {
        this.type = "space_update";
    }

    public SpaceStatusDTO(Long spaceId, String spaceCode, String status, String color,
                          Integer x, Integer y, Integer width, Integer height) {
        this.type = "space_update";
        this.spaceId = spaceId;
        this.spaceCode = spaceCode;
        this.name = spaceCode;
        this.status = status;
        this.color = color;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static String mapStatus(Integer dbStatus) {
        if (dbStatus == null) return "free";
        switch (dbStatus) {
            case 0: return "free";
            case 1: return "occupied";
            case 2: return "guiding";
            default: return "free";
        }
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getSpaceId() { return spaceId; }
    public void setSpaceId(Long spaceId) { this.spaceId = spaceId; }
    public String getSpaceCode() { return spaceCode; }
    public void setSpaceCode(String spaceCode) { this.spaceCode = spaceCode; this.name = spaceCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getX() { return x; }
    public void setX(Integer x) { this.x = x; }
    public Integer getY() { return y; }
    public void setY(Integer y) { this.y = y; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
