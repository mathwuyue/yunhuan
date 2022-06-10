from basic_core import *
from fastapi import FastAPI
from ml_core import *
from model import RequestData
from stats_core import *

app = FastAPI()


@app.post("/")
async def GetResult(request: RequestData):
    if request.modelId == 2:
        return cal_basic_statistics(request)
    elif request.modelId == 3:
        return cal_test_of_normality(request)
    elif request.modelId == 6:
        return cal_test_relative(request)
    elif request.modelId == 18:
        return cal_category_dist(request)
    elif request.modelId == 14:
        return cal_svm(request)
    elif request.modelId == 16:
        return cal_lightgbm(request)
