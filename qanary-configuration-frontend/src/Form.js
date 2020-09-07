import axios from "axios";
import React, {Component} from "react";
import FormElement from "./FormElement";
import './styling.css'

export default class Form extends Component{

    state = {
        error: null,
        configurations: {},
        serviceIp: process.env.REACT_APP_HOST+":"+process.env.REACT_APP_PORT
    };

    componentDidMount() {

        console.log("http://"+this.state.serviceIp+"/configuration");

        axios.get("http://"+this.state.serviceIp+"/configuration").then(
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

            // TODO make port configurable in docker command
            axios.post("http://"+this.state.serviceIp+"/configuration", input, {
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
                <input type={"submit"} value={"save"} className={'button'}/>
            </form>
        );
    }
}