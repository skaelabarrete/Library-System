function searchBook() {
    let keyword = document.getElementById("search").value;

    fetch(`/api/search?query=${keyword}`)
    .then(res => res.json())
    .then(data => {
        console.log(data);
    });
}

// real-time refresh
setInterval(() => {
    fetch('/api/books')
    .then(res => res.json())
    .then(data => console.log(data));
}, 3000);