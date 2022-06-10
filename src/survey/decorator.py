from functools import wraps

from flask import jsonify, session


# 登录验证
def login_required(role):
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kw):
            # return func(*args, **kw)
            auth_get = session.get("role")
            if not auth_get:
                return jsonify({"status": 0, "resp": "登录信息已失效"})
            if auth_get < role:
                return jsonify({"status": 0, "resp": "权限不足！"})
            return func(*args, **kw)

        return wrapper

    return decorator


# 检查查询是否符合标准
def check_valid(my_func):
    def wrapper():
        # check valid
        pass
