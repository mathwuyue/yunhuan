from pydantic import BaseModel
from typing import Optional, List, Any


class ChartData(BaseModel):
    chartId: int
    xaxis: List[str] = []
    yaxis: List[str] = []
    x: List[List[Any]] = []
    y: List[List[Any]] = []


class ResponseData(BaseModel):
    total: int
    chartArray: List[ChartData]
    info: Optional[str] = ""


class RequestData(BaseModel):
    modelId: int
    condition: Optional[str] = ""
    x_count: int
    y_count: int
    x: List[List[float]] = []
    y: List[List[float]] = []
