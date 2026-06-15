package com.smartpark.entity;

import java.io.Serializable;

public class ParkingSpace implements Serializable {
    private Long id;
    private String spaceCode;
    private String area;
    private Integer floor;
    private Integer status;
    private Integer xPos;
    private Integer yPos;
    private Integer width;
    private Integer height;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSpaceCode() { return spaceCode; }
    public void setSpaceCode(String spaceCode) { this.spaceCode = spaceCode; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getXPos() { return xPos; }
    public void setXPos(Integer xPos) { this.xPos = xPos; }
    public Integer getYPos() { return yPos; }
    public void setYPos(Integer yPos) { this.yPos = yPos; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
}
