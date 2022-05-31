from fastapi import FastAPI

import pandas as pd
import numpy as np
import scipy

from stats_core import *
from model import RequestData


app = FastAPI()


@app.post("/")
async def GetResult(request: RequestData):
    if request.modelId == 2:
        return cal_basic_statistics(request)
    elif request.modelId == 3:
        return cal_test_of_normality(request)
    elif request.modelId == 6:
        return cal_test_relative(request)
