## PyPI Repositories
- https://www.jfrog.com/confluence/display/RTF/PyPI+Repositories
- https://pypi.org/project/twine/

pip is already installed if you are using Python 2 >=2.7.9 or Python 3 >=3.4 downloaded from python.org 


## deploy

```
grab rt config pypi
python setup.py sdist upload -r artifactory
```


## install
```
grab rt config pypi-download
pip install demo
```
