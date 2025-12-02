import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="demo",
    version="0.0.1",
    author="boyu",
    author_email="boyu@private-domain",
    description="A small demo package",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/yubo/sampleproject",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
)
