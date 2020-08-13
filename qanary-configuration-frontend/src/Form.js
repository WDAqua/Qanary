import {useForm} from "react-hook-form";
import axios from "axios";
import React, {Component} from "react";
import FormElement from "./FormElement";

export default class Form extends Component{

    state = {
        error: null,
        configurations: {}
    };

    componentDidMount() {
        axios.get("http://localhost:8080/configuration").then(
            result => {
                this.setState({
                    loaded: true,
                    configurations: result.data
                });
                console.log("called");
                console.log(this.state.configurations);
            },
            error => {
                this.setState({
                    loaded: true,
                    error
                });
            }
        );
    }


    handleChange = event => {
       console.log(event);
       let dummy = this.state.configurations;
       dummy[event.name] = event.value;
       this.setState({dummy});
       console.log(this.state.configurations);
    }

    render() {

        const onSubmit = async (e) => {
            e.preventDefault();
            const input = this.state.configurations;

            axios.post("http://localhost:8080/configuration", input, {
                headers: {
                    'Content-Type': 'application/json'
                }}).then(
                (response) => {
                    console.log(response);
                })
        }

        return (
            <form onSubmit={onSubmit}>
                {Array.from(Object.keys(this.state.configurations)).map((key, i) => {
                    return <FormElement key={i} name={key} value={this.state.configurations[key]} onChange={this.handleChange}/>
                })}
                <input type={"submit"} value={"save"} style={{'margin-top': '10px', 'margin-left': '1rem'}}/>
            </form>
        );
    }

    /*

 onSubmit={handleSubmit(onSubmit)}

<input type="text" placeholder="my Qanary Pipeline" name="application_name" ref={register}/>
                <input type="text" placeholder="server_host" name="server_host" ref={register({required: true})}/>
                {errors.server_host && <p>host is required</p>}

const {register, handleSubmit, errors} = useForm();
        const onSubmit = async (data) => {
            console.log(data)
            const resp = await axios.post('https://localhost:8080/configuration', {data})
        }

     */


}